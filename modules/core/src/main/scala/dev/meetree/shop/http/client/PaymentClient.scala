package dev.meetree.shop.http.client

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{ Response, Status, Uri }

import dev.meetree.shop.config.types.PaymentConfig
import dev.meetree.shop.domain.order.{ PaymentError, PaymentId }
import dev.meetree.shop.domain.payment.Payment

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

object PaymentClient {
  def make[F[_]: JsonDecoder: MonadCancelThrow](
      config: PaymentConfig,
      client: Client[F]
  ): PaymentClient[F] = new PaymentClient[F] with Http4sClientDsl[F] {
    private val baseUri = config.uri.value

    def process(payment: Payment): F[PaymentId] =
      Uri
        .fromString(baseUri + "/payments")
        .liftTo[F]
        .map(uri => POST(payment, uri))
        .flatMap(request => client.run(request).use(decode(_)))

    private def decode(response: Response[F]): F[PaymentId] =
      response.status match {
        case Status.Ok | Status.Conflict =>
          response.asJsonDecode[PaymentId]
        case status                      =>
          PaymentError(Option(status.reason).getOrElse("Unknown"))
            .raiseError[F, PaymentId]
      }
  }
}
