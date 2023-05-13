package dev.meetree.shop

import cats.Show
import cats.kernel.{ Eq, Monoid }
import cats.syntax.contravariant._
import dev.profunktor.auth.jwt.JwtToken
import io.circe.{ Decoder, Encoder }
import squants.market.{ Currency, Money, USD }

package object domain extends JwtInstances with MoneyInstances

trait JwtInstances {
  implicit val jwtEq: Eq[JwtToken] = Eq.by(_.value)

  implicit val jwtShow: Show[JwtToken] =
    Show[String].contramap[JwtToken](_.value)

  implicit val jwtEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)
}

trait MoneyInstances {
  implicit val currencyEq: Eq[Currency] =
    Eq.by(c => (c.code, c.symbol, c.name))

  implicit val moneyEq: Eq[Money] =
    Eq.by(m => (m.amount, m.currency))

  implicit val moneyShow: Show[Money] = Show.fromToString

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyMonoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money                       = USD(0)
      def combine(x: Money, y: Money): Money = x + y
    }
}
