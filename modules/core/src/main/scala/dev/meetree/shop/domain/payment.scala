package dev.meetree.shop.domain

import derevo.cats.show
import derevo.circe.magnolia.encoder
import derevo.derive
import squants.market.Money

import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.card.Card

object payment {

  @derive(encoder, show)
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )
}
