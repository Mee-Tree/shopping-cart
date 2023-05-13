package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros.newtype

import java.util.UUID

import dev.meetree.shop.optics.uuid

object category {

  @derive(uuid, decoder, encoder, eqv, show)
  @newtype
  case class CategoryId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class CategoryName(value: String)

  @derive(decoder, encoder, eqv, show)
  case class Category(
      id: CategoryId,
      name: CategoryName
  )
}
