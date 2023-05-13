package dev.meetree.shop.domain

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.auto.autoUnwrap
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.control.NoStackTrace

import dev.meetree.shop.optics.uuid

object auth {

  @derive(uuid, decoder, encoder, eqv, show)
  @newtype
  case class UserId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class UserName(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class Password(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class EncryptedPassword(value: String)

  @derive(decoder, encoder, show)
  case class User(id: UserId, name: UserName)

  @derive(decoder, encoder)
  case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword) {
    def withoutPassword: User = User(id, name)
  }

  case class UserNotFound(username: UserName)    extends NoStackTrace
  case class UserNameInUse(username: UserName)   extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace
}
