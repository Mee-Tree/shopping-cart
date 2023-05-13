package dev.meetree.shop.http

import cats.syntax.either._

import java.util.UUID

import dev.meetree.shop.domain.item.ItemId
import dev.meetree.shop.domain.order.OrderId

object vars {

  protected class UUIDVar[A](f: UUID => A) {
    def unapply(str: String): Option[A] =
      Either.catchNonFatal(f(UUID.fromString(str))).toOption
  }

  object ItemIdVar  extends UUIDVar(ItemId.apply)
  object OrderIdVar extends UUIDVar(OrderId.apply)
}
