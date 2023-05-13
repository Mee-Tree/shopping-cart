package dev.meetree.shop.domain

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.cats._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.estatico.newtype.macros.newtype

import dev.meetree.shop.ext.refined._

object card {

  type Rgx = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"

  @derive(decoder, encoder, show)
  @newtype
  case class CardName(value: String Refined MatchesRegex[Rgx])

  @derive(decoder, encoder, show)
  @newtype
  case class CardNumber(value: Long Refined Size[16])

  @derive(decoder, encoder, show)
  @newtype
  case class CardExpiration(value: String Refined (Size[4] And ValidInt))

  @derive(decoder, encoder, show)
  @newtype
  case class CardCVV(value: Int Refined Size[3])

  @derive(decoder, encoder, show)
  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      ccv: CardCVV
  )
}
