package dev.meetree.shop.http.route

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import dev.meetree.shop.algebra.Categories

final case class CategoryRoutes[F[_]: Monad](
    categories: Categories[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(categories.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
