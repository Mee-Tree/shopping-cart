package dev.meetree.shop.module

import cats.effect.{ Resource, Temporal }
import dev.profunktor.redis4cats.RedisCommands
import skunk.Session

import dev.meetree.shop.algebra.{ Brands, Categories, HealthCheck, Items, Orders, ShoppingCart }
import dev.meetree.shop.config.types.ShoppingCartExpiration
import dev.meetree.shop.effect.GenUUID

sealed abstract class Services[F[_]] private (
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F],
    val cart: ShoppingCart[F],
    val healthCheck: HealthCheck[F]
)

object Services {
  def make[F[_]: GenUUID: Temporal](
      redis: RedisCommands[F, String, String],
      postgres: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): Services[F] = {
    val brands      = Brands.make[F](postgres)
    val categories  = Categories.make[F](postgres)
    val items       = Items.make[F](postgres)
    val orders      = Orders.make[F](postgres)
    val cart        = ShoppingCart.make[F](items, redis, cartExpiration)
    val healthCheck = HealthCheck.make[F](postgres, redis)

    new Services[F](brands, categories, items, orders, cart, healthCheck) {}
  }
}
