package dev.meetree.shop.http.route.admin

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }

import dev.meetree.shop.algebra.Brands
import dev.meetree.shop.domain.param.brand.BrandParam
import dev.meetree.shop.ext.http4s.refined._
import dev.meetree.shop.http.auth.user.AdminUser

final case class AdminBrandRoutes[F[_]: JsonDecoder: MonadThrow](
    brands: Brands[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case areq @ POST -> Root as _ =>
      areq.req.decodeR[BrandParam](create(_))
  }

  private def create(brand: BrandParam): F[Response[F]] =
    brands
      .create(brand.toDomain)
      .map(id => Map("brand_id" -> id))
      .flatMap(Created(_))

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
