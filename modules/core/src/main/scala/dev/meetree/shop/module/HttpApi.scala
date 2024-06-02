package dev.meetree.shop.module

import cats.effect.Async
import cats.syntax.all._
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.{ AutoSlash, CORS, RequestLogger, ResponseLogger, Timeout }
import org.http4s.{ HttpApp, HttpRoutes }

import scala.concurrent.duration.DurationInt

import dev.meetree.shop.http.auth.user.{ AdminUser, CommonUser }
import dev.meetree.shop.http.route._
import dev.meetree.shop.http.route.admin._
import dev.meetree.shop.http.route.auth._
import dev.meetree.shop.http.route.secure._

sealed abstract class HttpApi[F[_]: Async] private (
    services: Services[F],
    programs: Programs[F],
    security: Security[F]
) {
  private val adminMiddleware =
    JwtAuthMiddleware[F, AdminUser](security.adminJwtAuth.value, security.adminAuth.findUser)
  private val usersMiddleware =
    JwtAuthMiddleware[F, CommonUser](security.userJwtAuth.value, security.usersAuth.findUser)

  // Auth routes
  private val loginRoutes    = LoginRoutes[F](security.auth).routes
  private val logoutRoutes   = LogoutRoutes[F](security.auth).routes(usersMiddleware)
  private val registerRoutes = RegisterRoutes[F](security.auth).routes

  // Open routes
  private val healthRoutes   = HealthRoutes[F](services.healthCheck).routes
  private val brandRoutes    = BrandRoutes[F](services.brands).routes
  private val categoryRoutes = CategoryRoutes[F](services.categories).routes
  private val itemRoutes     = ItemRoutes[F](services.items).routes

  // Secure routes
  private val cartRoutes     = CartRoutes[F](services.cart).routes(usersMiddleware)
  private val checkoutRoutes = CheckoutRoutes[F](programs.checkout).routes(usersMiddleware)
  private val orderRoutes    = OrderRoutes[F](services.orders).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes    =
    AdminBrandRoutes[F](services.brands).routes(adminMiddleware)
  private val adminCategoryRoutes =
    AdminCategoryRoutes[F](services.categories).routes(adminMiddleware)
  private val adminItemRoutes     =
    AdminItemRoutes[F](services.items).routes(adminMiddleware)

  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> itemRoutes <+> brandRoutes <+>
      categoryRoutes <+> loginRoutes <+> registerRoutes <+>
      logoutRoutes <+> cartRoutes <+> orderRoutes <+>
      checkoutRoutes

  private val adminRoutes: HttpRoutes[F] =
    adminItemRoutes <+> adminBrandRoutes <+> adminCategoryRoutes

  private val routes: HttpRoutes[F] = Router(
    version.v1            -> openRoutes,
    version.v1 + "/admin" -> adminRoutes
  )

  private def chain[A](fs: (A => A)*): A => A =
    Function.chain(fs.toSeq)

  private val middleware: HttpRoutes[F] => HttpRoutes[F] =
    chain(
      AutoSlash(_),
      CORS.policy(_),
      Timeout(60.seconds)(_)
    )

  private val loggers: HttpApp[F] => HttpApp[F] =
    chain(
      RequestLogger.httpApp(true, true)(_),
      ResponseLogger.httpApp(true, true)(_)
    )

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}

object HttpApi {
  def make[F[_]: Async](
      services: Services[F],
      programs: Programs[F],
      security: Security[F]
  ): HttpApi[F] = new HttpApi[F](services, programs, security) {}
}
