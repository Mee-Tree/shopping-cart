package dev.meetree.shop.http.route

import cats.effect.IO
import cats.syntax.all._
import fs2.Stream
import org.http4s.Method.GET
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._

import dev.meetree.shop.algebra.Items
import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.brand.{ Brand, BrandName }
import dev.meetree.shop.domain.item.{ CreateItem, Item, ItemId, UpdateItem }
import dev.meetree.shop.generators.{ brandArb, itemArb }
import dev.meetree.suite.HttpSuite

object ItemRoutesSuite extends HttpSuite {

  def dataItems(items: List[Item]) = new TestItems {
    override def findAll: Stream[IO, Item]                  =
      Stream.emits(items)
    override def findBy(brand: BrandName): Stream[IO, Item] =
      Stream.emits(items.find(_.brand.name === brand).toList)
  }

  def failingItems(items: List[Item]) = new TestItems {
    override def findAll: Stream[IO, Item]                  =
      Stream.raiseError(DummyError) *> Stream.emits(items)
    override def findBy(brand: BrandName): Stream[IO, Item] =
      findAll
  }

  test("GET items succeeds") {
    forall { it: List[Item] =>
      val req    = GET(uri"/items")
      val routes = ItemRoutes[IO](dataItems(it)).routes
      expectHttpStream(routes, req)(it)
    }
  }

  test("GET items by brand succeeds") {
    forall { (it: List[Item], b: Brand) =>
      val req      = GET(uri"/items".withQueryParam("brand", b.name.value))
      val routes   = ItemRoutes[IO](dataItems(it)).routes
      val expected = it.find(_.brand.name === b.name).toList
      expectHttpStream(routes, req)(expected)
    }
  }

  test("GET items fails") {
    forall { it: List[Item] =>
      val req    = GET(uri"/items")
      val routes = ItemRoutes[IO](failingItems(it)).routes
      expectHttpStreamFailure(routes, req)
    }
  }
}

protected class TestItems extends Items[IO] {
  override def findAll: Stream[IO, Item]                  = Stream.empty
  override def findBy(brand: BrandName): Stream[IO, Item] = Stream.empty
  def findById(itemId: ItemId): IO[Option[Item]]          = IO.pure(none[Item])
  def create(item: CreateItem): IO[ItemId]                = ID.make[IO, ItemId]
  def update(item: UpdateItem): IO[Unit]                  = IO.unit
}
