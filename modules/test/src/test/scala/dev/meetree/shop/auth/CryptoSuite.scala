package dev.meetree.shop.auth

import cats.effect.IO
import eu.timepit.refined.auto._
import weaver.SimpleIOSuite

import dev.meetree.shop.auth.Crypto
import dev.meetree.shop.config.types.PasswordSalt
import dev.meetree.shop.domain.auth.Password

object CryptoSuite extends SimpleIOSuite {

  private val salt = PasswordSalt("53kr3t")

  test("password encoding and decoding roundtrip") {
    Crypto.make[IO](salt).map { crypto =>
      val ini = Password("simple123")
      val enc = crypto.encrypt(ini)
      val dec = crypto.decrypt(enc)
      expect.same(ini, dec)
    }
  }
}
