package dev.meetree.shop.algebra

import cats.MonadThrow
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands

import dev.meetree.shop.config.types.ShoppingCartExpiration
import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.cart.{ Cart, CartItem, CartTotal, Quantity }
import dev.meetree.shop.domain.item.ItemId
import dev.meetree.shop.domain.{ ID, moneyMonoid }
import dev.meetree.shop.effect.GenUUID

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {
  def make[F[_]: GenUUID: MonadThrow](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      expiration: ShoppingCartExpiration
  ): ShoppingCart[F] = new ShoppingCart[F] {
    private val ShoppingCartExpiration = expiration.value

    def add(
        userId: UserId,
        itemId: ItemId,
        quantity: Quantity
    ): F[Unit] =
      redis.hSet(userId.show, itemId.show, quantity.show) *>
        redis.expire(userId.show, ShoppingCartExpiration).void

    def get(userId: UserId): F[CartTotal] = {
      def readItem(key: String, value: String): F[Option[CartItem]] =
        for {
          id     <- ID.read[F, ItemId](key)
          qty    <- MonadThrow[F].catchNonFatal(Quantity(value.toInt))
          mbItem <- items.findById(id)
        } yield mbItem.map(_.cart(qty))

      for {
        fields <- redis.hGetAll(userId.show).map(_.toList)
        items  <- fields.traverseFilter { case (k, v) => readItem(k, v) }
        total   = items.foldMap(_.subTotal)
      } yield CartTotal(items, total)
    }

    def delete(userId: UserId): F[Unit] =
      redis.del(userId.show).void

    def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
      redis.hDel(userId.show, itemId.show).void

    def update(userId: UserId, cart: Cart): F[Unit] = {
      def updateQuantity(key: String): F[Unit] =
        for {
          id   <- ID.read[F, ItemId](key)
          mbQty = cart.items.get(id)
          _    <- mbQty.traverse_(qty => redis.hSet(userId.show, key, qty.show))
        } yield ()

      for {
        fields <- redis.hGetAll(userId.show).map(_.toList)
        _      <- fields.traverse_ { case (k, _) => updateQuantity(k) }
        _      <- redis.expire(userId.show, ShoppingCartExpiration).void
      } yield ()
    }
  }
}
