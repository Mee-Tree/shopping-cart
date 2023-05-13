package dev.meetree.shop.algebra

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.syntax.all._
import skunk.syntax.all._
import skunk.{ Command, Encoder, Query, Session, ~ }
import squants.market.Money

import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.cart.CartItem
import dev.meetree.shop.domain.order.{ Order, OrderId, PaymentId }
import dev.meetree.shop.effect.GenUUID

trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId]
}

object Orders {
  def make[F[_]: GenUUID: Concurrent](
      postgres: Resource[F, Session[F]],
      chunkSize: Int = 1024
  ): Orders[F] = new Orders[F] {
    import OrderSQL._

    def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
      postgres.use { session =>
        val prepared = session.prepare(selectByUserIdAndOrderId)
        prepared.use(pq => pq.option(userId ~ orderId))
      }

    def findBy(userId: UserId): F[List[Order]] =
      postgres.use { session =>
        val prepared = session.prepare(selectByUserId)
        prepared.use(pq => pq.stream(userId, chunkSize).compile.toList)
      }

    def create(
        userId: UserId,
        paymentId: PaymentId,
        items: NonEmptyList[CartItem],
        total: Money
    ): F[OrderId] =
      postgres.use { session =>
        val prepared = session.prepare(insertOrder)
        prepared.use { cmd =>
          for {
            id      <- ID.make[F, OrderId]
            itemsMap = items.toList.map(ci => ci.item.id -> ci.quantity).toMap
            order    = Order(id, paymentId, itemsMap, total)
            _       <- cmd.execute(userId ~ order)
          } yield id
        }
      }
  }
}

private object OrderSQL {
  import dev.meetree.shop.sql.codec.{ order, orderId, paymentId, orderItems }
  import dev.meetree.shop.sql.codec.{ userId, money }

  val selectByUserId: Query[UserId, Order] =
    sql"""
       SELECT * FROM orders
       WHERE user_id = $userId
       """.query(order)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
    sql"""
       SELECT * FROM orders
       WHERE user_id = $userId AND id = $orderId
       """.query(order)

  private val userIdWithOrder: Encoder[UserId ~ Order] =
    (orderId ~ userId ~ paymentId ~ orderItems ~ money).contramap {
      case uid ~ o => o.id ~ uid ~ o.paymentId ~ o.items ~ o.total
    }

  val insertOrder: Command[UserId ~ Order] =
    sql"""
       INSERT INTO orders
       VALUES ($userIdWithOrder)
       """.command
}
