package dev.meetree.shop.http.route.auth

import cats.Monad
import cats.syntax.all._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }

import dev.meetree.shop.algebra.Auth
import dev.meetree.shop.domain.auth.User
import dev.meetree.shop.http.auth.user.CommonUser

final case class LogoutRoutes[F[_]: Monad](
    auth: Auth[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case areq @ POST -> Root / "logout" as user =>
      val jwt = AuthHeaders.getBearerToken(areq.req)
      logout(jwt, user.value)
  }

  private def logout(jwt: Option[JwtToken], user: User): F[Response[F]] =
    jwt.traverse_(auth.logout(_, user.name)) >> NoContent()

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
