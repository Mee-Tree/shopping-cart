package dev.meetree.shop.auth

import cats.Monad
import cats.syntax.all._
import dev.profunktor.auth.jwt.{ JwtSecretKey, JwtToken, jwtEncode }
import eu.timepit.refined.auto.autoUnwrap
import io.circe.syntax._
import pdi.jwt.{ JwtAlgorithm, JwtClaim }

import dev.meetree.shop.config.types.{ JwtAccessTokenKeyConfig, TokenExpiration }
import dev.meetree.shop.effect.GenUUID

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

object Tokens {
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: JwtAccessTokenKeyConfig,
      expiration: TokenExpiration
  ): Tokens[F] = new Tokens[F] {
    def create: F[JwtToken] =
      for {
        id       <- GenUUID[F].make
        idJson    = id.asJson.noSpaces
        claim    <- jwtExpire.expiresIn(JwtClaim(idJson), expiration)
        secretKey = JwtSecretKey(config.secret)
        token    <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
  }
}
