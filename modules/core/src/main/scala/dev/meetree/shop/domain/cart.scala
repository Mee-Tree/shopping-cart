package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.circe.{ Decoder, Encoder }
import io.estatico.newtype.macros.newtype
import squants.market.{ Money, USD }

import scala.util.control.NoStackTrace

import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.item.{ Item, ItemId }

object cart {

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class Quantity(value: Int)

  @derive(eqv, show)
  @newtype
  case class Cart(items: Map[ItemId, Quantity])

  object Cart {
    implicit val jsonEncoder: Encoder[Cart] =
      Encoder.forProduct1("items")(_.items)

    implicit val jsonDecoder: Decoder[Cart] =
      Decoder.forProduct1("items")(Cart.apply)
  }

  @derive(decoder, encoder, eqv, show)
  case class CartItem(item: Item, quantity: Quantity) {
    def subTotal: Money = USD(item.price.amount * quantity.value)
  }

  @derive(decoder, encoder, eqv, show)
  case class CartTotal(items: List[CartItem], total: Money)

  @derive(decoder, encoder)
  case class CartNotFound(userId: UserId) extends NoStackTrace
}
