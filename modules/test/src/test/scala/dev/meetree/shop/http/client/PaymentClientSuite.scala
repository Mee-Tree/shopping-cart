package dev.meetree.shop.http.client

import cats.effect.IO
import eu.timepit.refined.auto._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{ HttpRoutes, Response }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import dev.meetree.shop.config.types.{ PaymentConfig, PaymentURI }
import dev.meetree.shop.domain.order.{ PaymentError, PaymentId }
import dev.meetree.shop.domain.payment.Payment
import dev.meetree.shop.generators.{ paymentArb, paymentIdArb }

object PaymentClientSuite extends SimpleIOSuite with Checkers {

  val config = PaymentConfig(PaymentURI("http://localhost"))

  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] { case POST -> Root / "payments" => mkResponse }
      .orNotFound

  test("Response Ok (200)") {
    forall { (pid: PaymentId, p: Payment) =>
      val client = Client.fromHttpApp(routes(Ok(pid)))

      PaymentClient
        .make[IO](config, client)
        .process(p)
        .map(expect.same(pid, _))
    }
  }

  test("Response Conflict (409)") {
    forall { (pid: PaymentId, p: Payment) =>
      val client = Client.fromHttpApp(routes(Conflict(pid)))

      PaymentClient
        .make[IO](config, client)
        .process(p)
        .map(expect.same(pid, _))
    }
  }

  test("Internal Server Error response (500)") {
    forall { p: Payment =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      PaymentClient
        .make[IO](config, client)
        .process(p)
        .attempt
        .map {
          case Left(e)  => expect.same(PaymentError("Internal Server Error"), e)
          case Right(_) => failure("expected payment error")
        }
    }
  }
}
