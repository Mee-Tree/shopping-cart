package dev.meetree.shop.retry

import cats.effect.Temporal
import cats.syntax.show._
import org.typelevel.log4cats.Logger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.{ RetryDetails, RetryPolicy, retryingOnAllErrors }

trait Retry[F[_]] {
  def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A]
}

object Retry {
  def apply[F[_]: Retry]: Retry[F] = implicitly

  implicit def forLoggerTemporal[F[_]: Logger: Temporal]: Retry[F] = new Retry[F] {

    override def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A] = {

      def onError(e: Throwable, details: RetryDetails): F[Unit] =
        details match {
          case WillDelayAndRetry(_, retries, _) =>
            Logger[F].error(show"#$retries Failed to process ${retriable} with ${e.getMessage}.")
          case GivingUp(retries, _)             =>
            Logger[F].error(show"Giving up on ${retriable} after $retries retries.")
        }

      retryingOnAllErrors[A](policy, onError)(fa)
    }
  }
}
