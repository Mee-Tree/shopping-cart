package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder, keyDecoder, keyEncoder }
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.market.Money

import java.util.UUID

import dev.meetree.shop.domain.brand.{ Brand, BrandId }
import dev.meetree.shop.domain.cart.{ CartItem, Quantity }
import dev.meetree.shop.domain.category.{ Category, CategoryId }
import dev.meetree.shop.optics.uuid

object item {

  @derive(uuid, keyDecoder, keyEncoder, decoder, encoder, eqv, show)
  @newtype
  case class ItemId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class ItemName(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class ItemDescription(value: String)

  @derive(decoder, encoder, eqv, show)
  case class Item(
      id: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: Brand,
      category: Category
  ) {
    def cart(quantity: Quantity): CartItem = CartItem(this, quantity)
  }

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  case class UpdateItem(
      id: ItemId,
      price: Money
  )
}
