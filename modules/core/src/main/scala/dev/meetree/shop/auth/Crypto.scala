package dev.meetree.shop.auth

import cats.effect.kernel.Sync
import cats.syntax.all._
import io.estatico.newtype.macros.newtype

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import javax.crypto.spec.{ PBEKeySpec, SecretKeySpec }
import javax.crypto.{ Cipher, SecretKeyFactory }
import scala.util.chaining._

import dev.meetree.shop.config.types.PasswordSalt
import dev.meetree.shop.domain.auth.{ EncryptedPassword, Password }

trait Crypto {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}

object Crypto {
  @newtype
  case class EncryptCipher(value: Cipher)

  @newtype
  case class DecryptCipher(value: Cipher)

  def make[F[_]: Sync](passwordSalt: PasswordSalt): F[Crypto] =
    Sync[F]
      .delay {
        val salt      = passwordSalt.secret.value.getBytes("UTF-8")
        val keySpec   = new PBEKeySpec("password".toCharArray(), salt, 65536, 256)
        val sKey      = SecretKeyFactory
          .getInstance("PBKDF2WithHmacSHA512")
          .generateSecret(keySpec)
        val sKeySpec  = new SecretKeySpec(sKey.getEncoded, "AES")
        val encrypter = Cipher
          .getInstance("AES/ECB/PKCS5Padding")
          .tap(_.init(Cipher.ENCRYPT_MODE, sKeySpec))
          .pipe(EncryptCipher(_))
        val decrypter = Cipher
          .getInstance("AES/ECB/PKCS5Padding")
          .tap(_.init(Cipher.DECRYPT_MODE, sKeySpec))
          .pipe(DecryptCipher(_))

        (encrypter, decrypter)
      }
      .map {
        case (ec, dc) =>
          new Crypto {
            def encrypt(password: Password): EncryptedPassword = {
              val b64    = Base64.getEncoder()
              val bytes  = password.value.getBytes(UTF_8)
              val result = new String(b64.encode(ec.value.doFinal(bytes)), UTF_8)
              EncryptedPassword(result)
            }

            def decrypt(password: EncryptedPassword): Password = {
              val b64    = Base64.getDecoder()
              val bytes  = b64.decode(password.value.getBytes(UTF_8))
              val result = new String(dc.value.doFinal(bytes), UTF_8)
              Password(result)
            }
          }
      }
}
