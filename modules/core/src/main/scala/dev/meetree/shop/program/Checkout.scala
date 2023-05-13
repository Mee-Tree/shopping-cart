package dev.meetree.shop.program

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryPolicy
import squants.market.Money

import scala.concurrent.duration.DurationInt

import dev.meetree.shop.algebra.{ Orders, ShoppingCart }
import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.card.Card
import dev.meetree.shop.domain.cart.{ CartItem, CartTotal }
import dev.meetree.shop.domain.order.{ EmptyCartError, OrderError, OrderId, PaymentError, PaymentId }
import dev.meetree.shop.domain.payment.Payment
import dev.meetree.shop.effect.Background
import dev.meetree.shop.http.client.PaymentClient
import dev.meetree.shop.retry.{ Retriable, Retry }

final case class Checkout[F[_]: MonadThrow: Retry: Background: Logger](
    payments: PaymentClient[F],
    cart: ShoppingCart[F],
    orders: Orders[F],
    policy: RetryPolicy[F]
) {

  private def ensureNonEmpty[A](as: List[A]): F[NonEmptyList[A]] =
    MonadThrow[F].fromOption(NonEmptyList.fromList(as), EmptyCartError)

  private def processPayment(payment: Payment): F[PaymentId] =
    Retry[F]
      .retry(policy, Retriable.Payments)(payments.process(payment))
      .adaptError { case e => PaymentError(Option(e.getMessage).getOrElse("Unknown")) }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] = {
    val action = Retry[F]
      .retry(policy, Retriable.Orders)(orders.create(userId, paymentId, items, total))
      .adaptError { case e => OrderError(e.getMessage) }

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.onError {
        case _ =>
          Logger[F].error(
            show"Failed to create order for ${paymentId}. Recheduling in background."
          ) *> Background[F].schedule(bgAction(fa), 1.hour)
      }

    bgAction(action)
  }

  def process(userId: UserId, card: Card): F[OrderId] =
    for {
      CartTotal(items, total) <- cart.get(userId)
      items                   <- ensureNonEmpty(items)

      payment = Payment(userId, total, card)
      pid    <- processPayment(payment)

      oid <- createOrder(userId, pid, items, total)
      _   <- cart.delete(userId).attempt.void
    } yield oid
}
