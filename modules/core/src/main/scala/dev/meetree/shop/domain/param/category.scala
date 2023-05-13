package dev.meetree.shop.domain.param

import derevo.cats.show
import derevo.derive
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined.refinedDecoder
import io.estatico.newtype.macros.newtype

import dev.meetree.shop.domain.category.CategoryName

object category {

  @derive(show)
  @newtype
  case class CategoryParam(name: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(name.toLowerCase.capitalize)
  }

  object CategoryParam {
    implicit val jsonDecoder: Decoder[CategoryParam] =
      Decoder.forProduct1("name")(CategoryParam.apply)
  }
}
