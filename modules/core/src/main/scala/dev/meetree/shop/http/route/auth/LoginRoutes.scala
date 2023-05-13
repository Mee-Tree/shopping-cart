package dev.meetree.shop.http.route.auth

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, Response }

import dev.meetree.shop.algebra.Auth
import dev.meetree.shop.domain.auth.{ InvalidPassword, UserNotFound }
import dev.meetree.shop.domain.jwtEncoder
import dev.meetree.shop.domain.param.auth.LoginUser
import dev.meetree.shop.ext.http4s.refined._

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](
    auth: Auth[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req
        .decodeR[LoginUser](login(_))
        .recoverWith {
          case _: UserNotFound | _: InvalidPassword => Forbidden()
        }
  }

  private def login(user: LoginUser): F[Response[F]] =
    auth
      .login(user.username.toDomain, user.password.toDomain)
      .flatMap(Ok(_))

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
