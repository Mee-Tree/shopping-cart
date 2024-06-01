package dev.meetree.shop.program

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import squants.market.{ Money, USD }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.util.control.NoStackTrace

import dev.meetree.shop.algebra.{ Orders, ShoppingCart }
import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.card.Card
import dev.meetree.shop.domain.cart.{ Cart, CartItem, CartTotal, Quantity }
import dev.meetree.shop.domain.item.ItemId
import dev.meetree.shop.domain.order.{ EmptyCartError, Order, OrderError, OrderId, PaymentError, PaymentId }
import dev.meetree.shop.domain.payment.Payment
import dev.meetree.shop.effect.{ Background, TestBackground }
import dev.meetree.shop.generators.{ cardArb, cartTotalArb, orderIdArb, paymentIdArb, userIdArb }
import dev.meetree.shop.http.client.PaymentClient
import dev.meetree.shop.retry.TestRetry

object CheckoutSuite extends SimpleIOSuite with Checkers {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.pure(paymentId)
    }

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.raiseError(PaymentError(""))
    }

  def recoveringClient(attemptsSoFar: Ref[IO, Int], paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        attemptsSoFar.get.flatMap {
          case n if n === 1 => IO.pure(paymentId)
          case _            => attemptsSoFar.update(_ + 1) *> IO.raiseError(PaymentError(""))
        }
    }

  val failingOrders: Orders[IO] = new TestOrders {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: NonEmptyList[CartItem],
        total: Money
    ): IO[OrderId] =
      IO.raiseError(OrderError("test"))
  }

  val emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit]   = IO.raiseError(new NoStackTrace {})
  }

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit]   = IO.unit
  }

  def successfulOrders(orderId: OrderId): Orders[IO] = new TestOrders {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: NonEmptyList[CartItem],
        total: Money
    ): IO[OrderId] =
      IO.pure(orderId)
  }

  implicit val bg: Background[IO]                = TestBackground.NoOp
  implicit val lg: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  test("empty cart") {
    forall { (uid: UserId, pid: PaymentId, oid: OrderId, c: Card) =>
      val payments = successfulClient(pid)
      val cart     = emptyCart
      val orders   = successfulOrders(oid)

      Checkout[IO](payments, cart, orders, retryPolicy)
        .process(uid, c)
        .attempt
        .map {
          case Left(EmptyCartError) => success
          case _                    => failure("Cart was not empty as expected")
        }
    }
  }

  test("unreachable payment client") {
    forall { (uid: UserId, oid: OrderId, ct: CartTotal, c: Card) =>
      val retries = Ref.of[IO, Option[GivingUp]](None)

      retries.flatMap { retries =>
        implicit val retry = TestRetry.givingUp(retries)
        val payments       = unreachableClient
        val cart           = successfulCart(ct)
        val orders         = successfulOrders(oid)

        Checkout[IO](payments, cart, orders, retryPolicy)
          .process(uid, c)
          .attempt
          .flatMap {
            case Left(PaymentError(_)) =>
              retries.get.map {
                case Some(g) => expect.same(MaxRetries, g.totalRetries)
                case None    => failure("expected GivingUp")
              }
            case _                     =>
              IO.pure(failure("Expected payment error"))
          }
      }
    }
  }

  test("failing payment client succeeds after one retry") {
    forall { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, c: Card) =>
      val retries  = Ref.of[IO, Option[WillDelayAndRetry]](None)
      val attempts = Ref.of[IO, Int](0)

      (retries, attempts).tupled.flatMap {
        case (retries, attempts) =>
          implicit val retry = TestRetry.recovering(retries)
          val payments       = recoveringClient(attempts, pid)
          val cart           = successfulCart(ct)
          val orders         = successfulOrders(oid)

          Checkout[IO](payments, cart, orders, retryPolicy)
            .process(uid, c)
            .attempt
            .flatMap {
              case Right(id) =>
                retries.get.map {
                  case Some(w) => expect.same(oid, id) |+| expect.same(0, w.retriesSoFar)
                  case None    => failure("Expected one retry")
                }
              case Left(_)   =>
                IO.pure(failure("Expected Payment Id"))
            }
      }
    }
  }

  test("cannot create order, run in the background") {
    forall { (uid: UserId, pid: PaymentId, ct: CartTotal, c: Card) =>
      val retries   = Ref.of[IO, Option[GivingUp]](None)
      val bgActions = Ref.of[IO, (Int, FiniteDuration)](0 -> 0.seconds)

      (retries, bgActions).tupled.flatMap {
        case (retries, bgActions) =>
          implicit val retry = TestRetry.givingUp(retries)
          implicit val bg    = TestBackground.counter(bgActions)
          val payments       = successfulClient(pid)
          val cart           = successfulCart(ct)
          val orders         = failingOrders

          Checkout[IO](payments, cart, orders, retryPolicy)
            .process(uid, c)
            .attempt
            .flatMap {
              case Left(OrderError(_)) =>
                (retries.get, bgActions.get).mapN {
                  case (Some(gu), cnt) =>
                    expect.same(1 -> 1.hour, cnt) |+| expect.same(MaxRetries, gu.totalRetries)
                  case _               =>
                    failure(s"Expected $MaxRetries retries and reschedule")
                }
              case _                   =>
                IO.pure(failure("Expected order error"))
            }
      }
    }
  }

  test("failing to delete cart does not affect checkout") {
    forall { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, c: Card) =>
      val payments = successfulClient(pid)
      val cart     = failingCart(ct)
      val orders   = successfulOrders(oid)

      Checkout[IO](payments, cart, orders, retryPolicy)
        .process(uid, c)
        .map(expect.same(oid, _))
    }
  }

  test("successful checkout") {
    forall { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, c: Card) =>
      val payments = successfulClient(pid)
      val cart     = successfulCart(ct)
      val orders   = successfulOrders(oid)

      Checkout[IO](payments, cart, orders, retryPolicy)
        .process(uid, c)
        .map(expect.same(oid, _))
    }
  }
}

protected class TestOrders() extends Orders[IO] {
  def get(userId: UserId, orderId: OrderId): IO[Option[Order]] = ???
  def findBy(userId: UserId): IO[List[Order]]                  = ???
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): IO[OrderId] = ???
}

protected class TestCart() extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
  def get(userId: UserId): IO[CartTotal]                                = ???
  def delete(userId: UserId): IO[Unit]                                  = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = ???
  def update(userId: UserId, cart: Cart): IO[Unit]                      = ???
}
