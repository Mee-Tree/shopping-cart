package dev.meetree.shop.module

import cats.effect.Temporal
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryPolicies.{ exponentialBackoff, limitRetries }
import retry.RetryPolicy

import dev.meetree.shop.config.types.CheckoutConfig
import dev.meetree.shop.effect.Background
import dev.meetree.shop.program.Checkout

sealed abstract class Programs[F[_]: Background: Logger: Temporal] private (
    config: CheckoutConfig,
    services: Services[F],
    clients: HttpClients[F]
) {
  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](config.retriesLimit.value) |+| exponentialBackoff[F](config.retriesBackoff)

  val checkout: Checkout[F] = Checkout[F](
    clients.payment,
    services.cart,
    services.orders,
    retryPolicy
  )
}

object Programs {
  def make[F[_]: Background: Logger: Temporal](
      config: CheckoutConfig,
      services: Services[F],
      clients: HttpClients[F]
  ): Programs[F] = new Programs[F](config, services, clients) {}
}
