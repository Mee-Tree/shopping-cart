package dev.meetree.shop.http.route.admin

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }

import dev.meetree.shop.algebra.Items
import dev.meetree.shop.domain.param.item.{ CreateItemParam, UpdateItemParam }
import dev.meetree.shop.ext.http4s.refined._
import dev.meetree.shop.http.auth.user.AdminUser

final case class AdminItemRoutes[F[_]: JsonDecoder: MonadThrow](
    items: Items[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case areq @ POST -> Root as _ =>
      areq.req.decodeR[CreateItemParam](create(_))

    case areq @ PUT -> Root as _ =>
      areq.req.decodeR[UpdateItemParam](update(_))
  }

  private def create(item: CreateItemParam): F[Response[F]] =
    items
      .create(item.toDomain)
      .map(id => Map("item_id" -> id))
      .flatMap(Created(_))

  private def update(item: UpdateItemParam): F[Response[F]] =
    items.update(item.toDomain) >> Ok()

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
