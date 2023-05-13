package dev.meetree.shop.http.route

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._

import dev.meetree.shop.algebra.Brands
import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.brand.{ Brand, BrandId, BrandName }
import dev.meetree.shop.generators.brandArb
import dev.meetree.suite.HttpSuite

object BrandRoutesSuite extends HttpSuite {

  def dataBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.pure(brands)
  }

  def failingBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.raiseError(DummyError) *> IO.pure(brands)
  }

  test("GET brands succeeds") {
    forall { bs: List[Brand] =>
      val req    = GET(uri"/brands")
      val routes = BrandRoutes[IO](dataBrands(bs)).routes
      expectHttpBodyAndStatus(routes, req)(bs, Status.Ok)
    }
  }

  test("GET brands fails") {
    forall { bs: List[Brand] =>
      val req    = GET(uri"/brands")
      val routes = BrandRoutes[IO](failingBrands(bs)).routes
      expectHttpFailure(routes, req)
    }
  }
}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[BrandId] = ID.make[IO, BrandId]
  def findAll: IO[List[Brand]]             = IO.pure(List.empty)
}
