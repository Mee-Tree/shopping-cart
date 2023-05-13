package dev.meetree.shop.ext.derevo

import derevo.{ Derivation, NewTypeDerivation }

import scala.annotation.implicitNotFound

trait Derive[F[_]] extends Derivation[F] with NewTypeDerivation[F] {
  def instance(implicit ev: NewTypeOnly): Nothing = ev.absurd

  @implicitNotFound("Only newtypes instances can be derived")
  abstract final class NewTypeOnly {
    def absurd: Nothing = ???
  }
}
