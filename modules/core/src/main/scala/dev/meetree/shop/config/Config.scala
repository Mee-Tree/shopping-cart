package dev.meetree.shop.config

import cats.effect.Async
import cats.syntax.all._
import ciris.refined.refTypeConfigDecoder
import ciris.{ ConfigValue, env }
import com.comcast.ip4s._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.DurationInt

import dev.meetree.shop.config.types._

object Config {

  def load[F[_]: Async]: F[AppConfig] =
    env("APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case AppEnvironment.Test =>
          default[F](
            RedisURI("redis://localhost"),
            PaymentURI("https://payments.free.beeceptor.com")
          )
        case AppEnvironment.Prod =>
          default[F](
            RedisURI("redis://10.123.154.176"),
            PaymentURI("https://payments.net/api")
          )
      }
      .load[F]

  private def default[F[_]](
      redisUri: RedisURI,
      paymentUri: PaymentURI
  ): ConfigValue[F, AppConfig] =
    (
      env("JWT_SECRET_KEY").as[JwtSecretKeyConfig].secret,
      env("ACCESS_TOKEN_SECRET_KEY").as[JwtAccessTokenKeyConfig].secret,
      env("ADMIN_USER_TOKEN").as[AdminUserTokenConfig].secret,
      env("PASSWORD_SALT").as[PasswordSalt].secret,
      env("POSTGRES_PASSWORD").as[NonEmptyString].secret
    ).parMapN {
      (
          jwtSecretKey,
          accessTokenConfig,
          adminToken,
          passwordSalt,
          postgresPassword
      ) =>
        AppConfig(
          AdminJwtConfig(jwtSecretKey, adminToken),
          accessTokenConfig,
          passwordSalt,
          TokenExpiration(30.minutes),
          ShoppingCartExpiration(30.minutes),
          CheckoutConfig(
            retriesLimit = 3,
            retriesBackoff = 10.milliseconds
          ),
          PaymentConfig(paymentUri),
          HttpClientConfig(
            timeout = 60.seconds,
            idleTimeInPool = 30.seconds
          ),
          PostgreSQLConfig(
            host = "localhost",
            port = 5432,
            user = "postgres",
            password = postgresPassword,
            database = "shop",
            max = 10
          ),
          RedisConfig(redisUri),
          HttpServerConfig(
            host = host"0.0.0.0",
            port = port"8080"
          )
        )
    }
}
