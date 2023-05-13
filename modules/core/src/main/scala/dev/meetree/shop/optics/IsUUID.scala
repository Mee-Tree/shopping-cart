package dev.meetree.shop.optics

import monocle.Iso

import java.util.UUID

import dev.meetree.shop.ext.derevo.Derive

trait IsUUID[A] {
  def iso: Iso[UUID, A]
}

object IsUUID {
  def apply[A](implicit uuid: IsUUID[A]): Iso[UUID, A] = uuid.iso

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val iso = Iso[UUID, UUID](identity)(identity)
  }
}

object uuid extends Derive[IsUUID]
