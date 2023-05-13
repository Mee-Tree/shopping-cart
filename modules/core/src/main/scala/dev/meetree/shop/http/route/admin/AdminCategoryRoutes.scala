package dev.meetree.shop.http.route.admin

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }

import dev.meetree.shop.algebra.Categories
import dev.meetree.shop.domain.param.category.CategoryParam
import dev.meetree.shop.ext.http4s.refined._
import dev.meetree.shop.http.auth.user.AdminUser

final case class AdminCategoryRoutes[F[_]: JsonDecoder: MonadThrow](
    categories: Categories[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/categories"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case areq @ POST -> Root as _ =>
      areq.req.decodeR[CategoryParam](create(_))
  }

  private def create(category: CategoryParam): F[Response[F]] =
    categories
      .create(category.toDomain)
      .map(id => Map("category_id" -> id))
      .flatMap(Created(_))

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
