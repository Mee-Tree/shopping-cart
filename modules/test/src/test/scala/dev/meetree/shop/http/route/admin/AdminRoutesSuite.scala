package dev.meetree.shop.http.route.admin

import cats.data.Kleisli
import cats.effect.IO
import fs2.Stream
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.Status
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._

import dev.meetree.shop.algebra.{ Brands, Items }
import dev.meetree.shop.domain.brand.{ Brand, BrandId, BrandName }
import dev.meetree.shop.domain.item.{ CreateItem, Item, ItemId, UpdateItem }
import dev.meetree.shop.domain.param.brand.BrandParam
import dev.meetree.shop.domain.param.item.CreateItemParam
import dev.meetree.shop.generators.{ adminUserArb, brandIdArb, brandParamArb, createItemParamArb, itemIdArb }
import dev.meetree.shop.http.auth.user.AdminUser
import dev.meetree.suite.HttpSuite

object AdminRoutesSuite extends HttpSuite {

  def authMiddleware(authUser: AdminUser): AuthMiddleware[IO, AdminUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  test("POST create brand") {
    forall { (id: BrandId, u: AdminUser, bp: BrandParam) =>
      val req      = POST(bp, uri"/brands")
      val routes   = AdminBrandRoutes[IO](new TestBrands(id)).routes(authMiddleware(u))
      val expected = Map("brand_id" -> id).asJson
      expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }

  test("POST create item") {
    forall { (id: ItemId, u: AdminUser, cip: CreateItemParam) =>
      val req      = POST(cip, uri"/items")
      val routes   = AdminItemRoutes[IO](new TestItems(id)).routes(authMiddleware(u))
      val expected = Map("item_id" -> id).asJson
      expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }
}

protected class TestBrands(id: BrandId) extends Brands[IO] {
  def findAll: IO[List[Brand]]             = ???
  def create(name: BrandName): IO[BrandId] = IO.pure(id)
}

protected class TestItems(id: ItemId) extends Items[IO] {
  def findAll: Stream[IO, Item]                  = ???
  def findBy(brand: BrandName): Stream[IO, Item] = ???
  def findById(itemId: ItemId): IO[Option[Item]] = ???
  def create(item: CreateItem): IO[ItemId]       = IO.pure(id)
  def update(item: UpdateItem): IO[Unit]         = IO.unit
}
