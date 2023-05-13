package dev.meetree.shop.domain.param

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.cats._
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.estatico.newtype.macros.newtype
import squants.market.USD

import java.util.UUID

import dev.meetree.shop.domain.brand.BrandId
import dev.meetree.shop.domain.category.CategoryId
import dev.meetree.shop.domain.item.{ CreateItem, ItemDescription, ItemId, ItemName, UpdateItem }

object item {

  @derive(encoder, decoder, show)
  @newtype
  case class ItemIdParam(value: String Refined Uuid)

  @derive(encoder, decoder, show)
  @newtype
  case class ItemNameParam(value: NonEmptyString)

  @derive(encoder, decoder, show)
  @newtype
  case class ItemDescriptionParam(value: NonEmptyString)

  @derive(encoder, decoder, show)
  @newtype
  case class PriceParam(value: String Refined ValidBigDecimal)

  @derive(encoder, decoder, show)
  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: PriceParam,
      brandId: BrandId,
      categoryId: CategoryId
  ) {
    def toDomain: CreateItem =
      CreateItem(
        ItemName(name.value),
        ItemDescription(description.value),
        USD(BigDecimal(price.value)),
        brandId,
        categoryId
      )
  }

  @derive(encoder, decoder, show)
  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toDomain: UpdateItem =
      UpdateItem(
        ItemId(UUID.fromString(id.value)),
        USD(BigDecimal(price.value))
      )
  }
}
