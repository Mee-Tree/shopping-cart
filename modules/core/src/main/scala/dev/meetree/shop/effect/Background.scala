package dev.meetree.shop.effect

import cats.effect.Temporal
import cats.effect.std.Supervisor
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

trait Background[F[_]] {
  def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit]
}

object Background {
  def apply[F[_]: Background]: Background[F] = implicitly

  implicit def forSupervisorTemporal[F[_]](implicit
      S: Supervisor[F],
      T: Temporal[F]
  ): Background[F] = new Background[F] {

    override def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit] =
      S.supervise(T.sleep(duration) *> fa).void
  }
}
