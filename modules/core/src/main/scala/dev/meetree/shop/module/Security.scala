package dev.meetree.shop.module

import cats.ApplicativeThrow
import cats.effect.{ Resource, Sync }
import cats.syntax.all._
import dev.profunktor.auth.jwt.{ JwtAuth, JwtToken, jwtDecode }
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.auto.autoUnwrap
import io.circe.parser.{ decode => jsonDecode }
import pdi.jwt.JwtAlgorithm
import skunk.Session

import dev.meetree.shop.algebra.{ Auth, Users, UsersAuth }
import dev.meetree.shop.auth.{ Crypto, JwtExpire, Tokens }
import dev.meetree.shop.config.types.AppConfig
import dev.meetree.shop.domain.auth.{ User, UserId, UserName }
import dev.meetree.shop.http.auth.user.{ AdminJwtAuth, AdminUser, ClaimContent, CommonUser, UserJwtAuth }

sealed abstract class Security[F[_]] private (
    val auth: Auth[F],
    val adminAuth: UsersAuth[F, AdminUser],
    val usersAuth: UsersAuth[F, CommonUser],
    val adminJwtAuth: AdminJwtAuth,
    val userJwtAuth: UserJwtAuth
)

object Security {
  def make[F[_]: Sync](
      config: AppConfig,
      postgres: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {
    val adminJwtAuth =
      AdminJwtAuth(JwtAuth.hmac(config.adminJwtConfig.secretKey.value.secret, JwtAlgorithm.HS256))
    val userJwtAuth  =
      UserJwtAuth(JwtAuth.hmac(config.accessTokenConfig.value.secret, JwtAlgorithm.HS256))
    val adminToken   =
      JwtToken(config.adminJwtConfig.adminToken.value.secret)

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content    <- ApplicativeThrow[F].fromEither(jsonDecode[ClaimContent](adminClaim.content))
      adminUser   = AdminUser(User(UserId(content.id), UserName("admin")))
      jwtExpire  <- JwtExpire.make[F]
      tokens      = Tokens.make[F](jwtExpire, config.accessTokenConfig.value, config.tokenExpiration)
      crypto     <- Crypto.make[F](config.passwordSalt.value)
      users       = Users.make[F](postgres)
      auth        = Auth.make[F](config.tokenExpiration, tokens, users, redis, crypto)
      adminAuth   = UsersAuth.admin[F](adminToken, adminUser)
      usersAuth   = UsersAuth.common[F](redis)
    } yield new Security[F](auth, adminAuth, usersAuth, adminJwtAuth, userJwtAuth) {}
  }
}
