package dev.meetree.shop.auth

import cats.effect.Sync
import cats.syntax.all._
import pdi.jwt.JwtClaim

import dev.meetree.shop.config.types.TokenExpiration
import dev.meetree.shop.effect.JwtClock

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, expiration: TokenExpiration): F[JwtClaim]
}

object JwtExpire {
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc.map { implicit jClock =>
      new JwtExpire[F] {
        def expiresIn(claim: JwtClaim, expiration: TokenExpiration): F[JwtClaim] =
          Sync[F].delay(claim.issuedNow.expiresIn(expiration.value.toMillis))
      }
    }
}
