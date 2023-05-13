package dev.meetree.shop.domain.param

import derevo.circe.magnolia.decoder
import derevo.derive
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.refinedDecoder
import io.estatico.newtype.macros.newtype

import dev.meetree.shop.domain.auth.{ Password, UserName }

object auth {

  @derive(decoder)
  @newtype
  case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.toLowerCase)
  }

  @derive(decoder)
  @newtype
  case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value)
  }

  @derive(decoder)
  case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )

  @derive(decoder)
  case class RegisterUser(
      username: UserNameParam,
      password: PasswordParam
  )
}
