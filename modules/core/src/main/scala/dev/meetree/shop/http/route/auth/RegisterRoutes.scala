package dev.meetree.shop.http.route.auth

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, Response }

import dev.meetree.shop.algebra.Auth
import dev.meetree.shop.domain.auth.UserNameInUse
import dev.meetree.shop.domain.jwtEncoder
import dev.meetree.shop.domain.param.auth.RegisterUser
import dev.meetree.shop.ext.http4s.refined._

final case class RegisterRoutes[F[_]: JsonDecoder: MonadThrow](
    auth: Auth[F]
) extends Http4sDsl[F] {

  private[route] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      req
        .decodeR[RegisterUser](newUser(_))
        .recoverWith {
          case UserNameInUse(u) => Conflict(u.show)
        }
  }

  private def newUser(user: RegisterUser): F[Response[F]] =
    auth
      .newUser(user.username.toDomain, user.password.toDomain)
      .flatMap(Created(_))

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
