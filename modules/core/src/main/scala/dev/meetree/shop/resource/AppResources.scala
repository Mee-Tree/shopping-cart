package dev.meetree.shop.resource

import cats.effect.std.Console
import cats.effect.{ Concurrent, Resource }
import cats.syntax.all._
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import eu.timepit.refined.auto.autoUnwrap
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk.codec.text.text
import skunk.implicits._
import skunk.{ Session, SessionPool }

import dev.meetree.shop.config.types.{ AppConfig, PostgreSQLConfig, RedisConfig }

sealed abstract class AppResources[F[_]](
    val client: Client[F],
    val postgres: Resource[F, Session[F]],
    val redis: RedisCommands[F, String, String]
)

object AppResources {

  def make[F[_]: Concurrent: Console: Logger: MkHttpClient: MkRedis: Network](
      config: AppConfig
  ): Resource[F, AppResources[F]] = {

    def checkPostgresConnection(
        postgres: Resource[F, Session[F]]
    ): F[Unit] =
      postgres.use { session =>
        session.unique(sql"select version();".query(text)).flatMap { v =>
          Logger[F].info(s"Connected to Postgres $v")
        }
      }

    def checkRedisConnection(
        redis: RedisCommands[F, String, String]
    ): F[Unit] =
      redis.info.flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to Redis $v")
        }
      }

    def mkPostgreSqlResource(cfg: PostgreSQLConfig): SessionPool[F] =
      Session
        .pooled[F](
          host = cfg.host.value,
          port = cfg.port.value,
          user = cfg.user.value,
          password = Some(cfg.password.value),
          database = cfg.database.value,
          max = cfg.max.value
        )
        .evalTap(checkPostgresConnection)

    def mkRedisResource(cfg: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(cfg.uri.value).evalTap(checkRedisConnection)

    (
      MkHttpClient[F].newEmber(config.httpClientConfig),
      mkPostgreSqlResource(config.postgreSQL),
      mkRedisResource(config.redis)
    )
      .parMapN(new AppResources[F](_, _, _) {})
  }
}
