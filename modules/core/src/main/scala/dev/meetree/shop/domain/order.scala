package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.market.Money

import java.util.UUID
import scala.util.control.NoStackTrace

import dev.meetree.shop.domain.cart.Quantity
import dev.meetree.shop.domain.item.ItemId
import dev.meetree.shop.optics.uuid

object order {

  @derive(uuid, decoder, encoder, eqv, show)
  @newtype
  case class OrderId(value: UUID)

  @derive(uuid, decoder, encoder, eqv, show)
  @newtype
  case class PaymentId(value: UUID)

  @derive(decoder, encoder, eqv)
  case class Order(
      id: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  @derive(show)
  case object EmptyCartError extends NoStackTrace

  @derive(show)
  sealed trait OrderOrPaymentError extends NoStackTrace {
    def cause: String
  }

  @derive(show)
  case class OrderError(cause: String)   extends OrderOrPaymentError
  @derive(show)
  case class PaymentError(cause: String) extends OrderOrPaymentError
}
