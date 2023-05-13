package dev.meetree.shop.algebra

import cats.effect.Temporal
import cats.effect.implicits._
import cats.effect.kernel.Resource
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import skunk.codec.all.int4
import skunk.syntax.all._
import skunk.{ Query, Session, Void }

import scala.concurrent.duration.DurationInt

import dev.meetree.shop.domain.healthcheck.{ AppStatus, PostgresStatus, RedisStatus, Status }

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object HealthCheck {
  def make[F[_]: Temporal](
      postgres: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): HealthCheck[F] = new HealthCheck[F] {

    val redisHealth: F[RedisStatus] =
      redis.ping
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status.bool(_))
        .orElse(Status.Unreachable.pure[F].widen)
        .map(RedisStatus(_))

    private val ping: Query[Void, Int] =
      sql"SELECT pid FROM pg_stat_activity".query(int4)

    val postgresHealth: F[PostgresStatus] =
      postgres
        .use(_.execute(ping))
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status.bool(_))
        .orElse(Status.Unreachable.pure[F].widen)
        .map(PostgresStatus(_))

    val status: F[AppStatus] =
      (redisHealth, postgresHealth)
        .parMapN(AppStatus(_, _))
  }
}
