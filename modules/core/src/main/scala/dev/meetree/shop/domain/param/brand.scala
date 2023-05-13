package dev.meetree.shop.domain.param

import derevo.cats.show
import derevo.derive
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.circe.{ Decoder, Encoder }
import io.estatico.newtype.macros.newtype

import dev.meetree.shop.domain.brand.BrandName
import dev.meetree.shop.ext.http4s.queryParam
import dev.meetree.shop.ext.http4s.refined._

object brand {

  @derive(queryParam, show)
  @newtype
  case class BrandParam(name: NonEmptyString) {
    def toDomain: BrandName = BrandName(name.toLowerCase.capitalize)
  }

  object BrandParam {
    implicit val jsonEncoder: Encoder[BrandParam] =
      Encoder.forProduct1("name")(_.name)

    implicit val jsonDecoder: Decoder[BrandParam] =
      Decoder.forProduct1("name")(BrandParam.apply)
  }
}
