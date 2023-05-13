package dev.meetree.shop.http.route.secure

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.Method.{ GET, POST }
import org.http4s.Status
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import squants.market.USD

import dev.meetree.shop.algebra.ShoppingCart
import dev.meetree.shop.domain.auth.UserId
import dev.meetree.shop.domain.cart.{ Cart, CartTotal, Quantity }
import dev.meetree.shop.domain.item.ItemId
import dev.meetree.shop.generators.{ cartArb, cartTotalArb, commonUserArb }
import dev.meetree.shop.http.auth.user.CommonUser
import dev.meetree.suite.HttpSuite

object CartRoutesSuite extends HttpSuite {

  def authMiddleware(authUser: CommonUser): AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal) = new TestShoppingCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
  }

  test("GET shopping cart succeeds") {
    forall { (u: CommonUser, ct: CartTotal) =>
      val req    = GET(uri"/cart")
      val routes = CartRoutes[IO](dataCart(ct)).routes(authMiddleware(u))
      expectHttpBodyAndStatus(routes, req)(ct, Status.Ok)
    }
  }

  test("POST add item to shopping cart succeeds") {
    forall { (u: CommonUser, c: Cart) =>
      val req    = POST(c, uri"/cart")
      val routes = CartRoutes[IO](new TestShoppingCart).routes(authMiddleware(u))
      expectHttpStatus(routes, req)(Status.Created)
    }
  }
}

protected class TestShoppingCart extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  def get(userId: UserId): IO[CartTotal]                                =
    IO.pure(CartTotal(List.empty, USD(0)))
  def delete(userId: UserId): IO[Unit]                                  = IO.unit
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = IO.unit
  def update(userId: UserId, cart: Cart): IO[Unit]                      = IO.unit
}
