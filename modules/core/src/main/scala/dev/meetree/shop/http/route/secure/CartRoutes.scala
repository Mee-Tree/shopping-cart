package dev.meetree.shop.http.route.secure

import cats.Monad
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }

import dev.meetree.shop.algebra.ShoppingCart
import dev.meetree.shop.domain.cart.Cart
import dev.meetree.shop.http.auth.user.CommonUser
import dev.meetree.shop.http.vars.ItemIdVar

final case class CartRoutes[F[_]: JsonDecoder: Monad](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case GET -> Root as user =>
      Ok(shoppingCart.get(user.value.id))

    case areq @ POST -> Root as user =>
      for {
        cart  <- areq.req.asJsonDecode[Cart]
        items  = cart.items.toList
        userId = user.value.id
        _     <- items.traverse_ { case (id, qty) => shoppingCart.add(userId, id, qty) }
        resp  <- Created()
      } yield resp

    case areq @ PUT -> Root as user =>
      for {
        cart  <- areq.req.asJsonDecode[Cart]
        userId = user.value.id
        _     <- shoppingCart.update(userId, cart)
        resp  <- Ok()
      } yield resp

    case DELETE -> Root / ItemIdVar(itemId) as user =>
      val userId = user.value.id
      shoppingCart.removeItem(userId, itemId) >> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
