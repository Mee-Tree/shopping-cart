package dev.meetree.shop.http.route.secure

import cats.Monad
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }

import dev.meetree.shop.algebra.Orders
import dev.meetree.shop.http.auth.user.CommonUser
import dev.meetree.shop.http.vars

final case class OrderRoutes[F[_]: Monad](
    orders: Orders[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case GET -> Root as user =>
      Ok(orders.findBy(user.value.id))

    case GET -> Root / vars.OrderIdVar(orderId) as user =>
      Ok(orders.get(user.value.id, orderId))
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
