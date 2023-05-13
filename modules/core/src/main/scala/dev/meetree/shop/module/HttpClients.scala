package dev.meetree.shop.module

import cats.effect.MonadCancelThrow
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client

import dev.meetree.shop.config.types.PaymentConfig
import dev.meetree.shop.http.client.PaymentClient

sealed trait HttpClients[F[_]] {
  val payment: PaymentClient[F]
}

object HttpClients {
  def make[F[_]: JsonDecoder: MonadCancelThrow](
      config: PaymentConfig,
      client: Client[F]
  ): HttpClients[F] = new HttpClients[F] {
    val payment: PaymentClient[F] = PaymentClient.make[F](config, client)
  }
}
