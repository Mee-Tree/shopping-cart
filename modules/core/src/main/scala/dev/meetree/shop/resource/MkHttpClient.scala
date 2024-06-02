package dev.meetree.shop.resource

import cats.effect.kernel.{ Async, Resource }
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import dev.meetree.shop.config.types.HttpClientConfig

trait MkHttpClient[F[_]] {
  def newEmber(config: HttpClientConfig): Resource[F, Client[F]]
}

object MkHttpClient {
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async: Network]: MkHttpClient[F] =
    new MkHttpClient[F] {
      def newEmber(config: HttpClientConfig): Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .withTimeout(config.timeout)
          .withIdleTimeInPool(config.idleTimeInPool)
          .build
    }
}
