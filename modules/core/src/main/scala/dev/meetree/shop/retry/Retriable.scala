package dev.meetree.shop.retry

import derevo.cats.show
import derevo.derive

@derive(show)
sealed trait Retriable extends Product with Serializable

object Retriable {
  case object Orders   extends Retriable
  case object Payments extends Retriable
}
