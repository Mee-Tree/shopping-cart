package dev.meetree.shop.http.route

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import dev.meetree.shop.algebra.Items
import dev.meetree.shop.domain.param.brand.BrandParam

final case class ItemRoutes[F[_]: Monad](
    items: Items[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/items"

  object BrandQueryParam extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? BrandQueryParam(brand) =>
      Ok(brand.fold(items.findAll)(b => items.findBy(b.toDomain)))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
