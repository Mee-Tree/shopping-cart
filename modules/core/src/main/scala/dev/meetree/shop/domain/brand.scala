package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros.newtype

import java.util.UUID

import dev.meetree.shop.optics.uuid

object brand {

  @derive(uuid, decoder, encoder, eqv, show)
  @newtype
  case class BrandId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class BrandName(value: String)

  @derive(decoder, encoder, eqv, show)
  case class Brand(
      id: BrandId,
      name: BrandName
  )
}
