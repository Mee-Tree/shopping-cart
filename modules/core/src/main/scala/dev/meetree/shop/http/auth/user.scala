package dev.meetree.shop.http.auth

import derevo.cats.show
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.Decoder
import io.estatico.newtype.macros.newtype

import java.util.UUID

import dev.meetree.shop.domain.auth.User

object user {

  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @derive(show)
  @newtype
  case class CommonUser(value: User)

  @derive(show)
  @newtype
  case class AdminUser(value: User)

  @newtype
  case class ClaimContent(id: UUID)

  object ClaimContent {
    implicit val jsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("id")(ClaimContent.apply)
  }
}
