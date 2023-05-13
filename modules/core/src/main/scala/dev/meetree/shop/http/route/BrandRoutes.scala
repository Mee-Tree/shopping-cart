package dev.meetree.shop.http.route

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import dev.meetree.shop.algebra.Brands

final case class BrandRoutes[F[_]: Monad](
    brands: Brands[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/brands"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(brands.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
