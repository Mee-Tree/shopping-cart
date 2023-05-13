package dev.meetree.shop

import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import cats.effect.{ IO, IOApp }
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import dev.meetree.shop.config.Config
import dev.meetree.shop.config.types.AppConfig
import dev.meetree.shop.effect.Background
import dev.meetree.shop.module.{ HttpApi, HttpClients, Programs, Security, Services }
import dev.meetree.shop.resource.{ AppResources, MkHttpServer }

object Main extends IOApp.Simple {

  implicit val logger = Slf4jLogger.getLogger[IO]

  private def app(cfg: AppConfig)(implicit bg: Background[IO]): Resource[IO, Server] =
    for {
      res      <- AppResources.make[IO](cfg)
      security <- Resource.eval(Security.make[IO](cfg, res.postgres, res.redis))
      clients   = HttpClients.make[IO](cfg.paymentConfig, res.client)
      services  = Services.make[IO](res.redis, res.postgres, cfg.cartExpiration)
      programs  = Programs.make[IO](cfg.checkoutConfig, services, clients)
      api       = HttpApi.make[IO](services, programs, security)
      server   <- MkHttpServer[IO].newEmber(cfg.httpServerConfig, api.httpApp)
    } yield server

  override def run: IO[Unit] =
    for {
      cfg <- Config.load[IO]
      _   <- Logger[IO].info(s"Loaded config $cfg")
      _   <- Supervisor[IO].use(implicit sp => app(cfg).useForever)
    } yield ()
}
