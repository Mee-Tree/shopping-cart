package dev.meetree.shop.http.route.secure

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }

import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.card.Card
import dev.meetree.shop.domain.cart.CartNotFound
import dev.meetree.shop.domain.order.{ EmptyCartError, OrderOrPaymentError }
import dev.meetree.shop.ext.http4s.refined._
import dev.meetree.shop.http.auth.user.CommonUser
import dev.meetree.shop.program.Checkout

final case class CheckoutRoutes[F[_]: JsonDecoder: MonadThrow](
    checkout: Checkout[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case areq @ POST -> Root as user =>
      areq.req
        .decodeR[Card](process(user.value.id, _))
        .recoverWith {
          case CartNotFound(userId)   => NotFound(show"Cart not found for user: $userId")
          case EmptyCartError         => BadRequest("Shopping cart is empty!")
          case e: OrderOrPaymentError => BadRequest(e.show)
        }
  }

  private def process(userId: UserId, card: Card): F[Response[F]] =
    checkout.process(userId, card).flatMap(Created(_))

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
