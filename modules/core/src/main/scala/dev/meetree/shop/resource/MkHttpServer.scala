package dev.meetree.shop.resource

import cats.effect.kernel.{ Async, Resource }
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

import dev.meetree.shop.config.types.HttpServerConfig

trait MkHttpServer[F[_]] {
  def newEmber(config: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server]
}

object MkHttpServer {
  def apply[F[_]: MkHttpServer]: MkHttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"""
      |${Banner.mkString("\n")}
      |HTTP Server started at ${s.address}
      |""".stripMargin
    )

  implicit def forAsyncLogger[F[_]: Async: Logger: Network]: MkHttpServer[F] =
    new MkHttpServer[F] {
      def newEmber(config: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(httpApp)
          .build
          .evalTap(showEmberBanner[F])
    }
}
