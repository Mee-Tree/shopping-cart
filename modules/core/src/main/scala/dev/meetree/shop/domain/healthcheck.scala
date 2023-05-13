package dev.meetree.shop.domain

import derevo.cats.eqv
import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import monocle.Iso

object healthcheck {

  @derive(eqv)
  sealed trait Status extends Product with Serializable

  object Status {
    case object Okay        extends Status
    case object Unreachable extends Status

    def bool: Iso[Status, Boolean] =
      Iso[Status, Boolean] {
        case Okay        => true
        case Unreachable => false
      }(if (_) Okay else Unreachable)

    implicit val jsonEncoder: Encoder[Status] =
      Encoder.forProduct1("status")(_.toString.toLowerCase)
  }

  @derive(encoder)
  @newtype
  case class RedisStatus(status: Status)

  @derive(encoder)
  @newtype
  case class PostgresStatus(status: Status)

  @derive(encoder)
  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}
