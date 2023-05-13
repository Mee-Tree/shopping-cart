package dev.meetree.shop.domain

import cats.Functor
import cats.syntax.functor._

import dev.meetree.shop.effect.GenUUID
import dev.meetree.shop.optics.IsUUID

object ID {
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].make.map(IsUUID[A].get)

  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A].get)
}
