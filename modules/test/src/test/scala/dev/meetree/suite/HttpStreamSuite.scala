package dev.meetree.suite

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import io.circe._
import io.circe.jawn.CirceSupportParser
import io.circe.syntax._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.typelevel.jawn.Facade
import org.typelevel.jawn.fs2._
import weaver.scalacheck.Checkers
import weaver.{ Expectations, SimpleIOSuite }

trait HttpStreamSuite extends SimpleIOSuite with Checkers {

  private implicit val facade: Facade[Json] = new CirceSupportParser(None, false).facade

  def jsonStream(routes: HttpRoutes[IO])(req: Request[IO]): Stream[IO, Json] =
    Client
      .fromHttpApp(routes.orNotFound)
      .stream(req)
      .flatMap(resp => resp.body.chunks.parseJsonStream)

  def expectHttpStream[A: Encoder](routes: HttpRoutes[IO], req: Request[IO])(
      expectedBody: List[A]
  ): IO[Expectations] =
    jsonStream(routes)(req)
      .zip(Stream.emits(expectedBody).map(_.asJson))
      .map { case (got, expected) => expect.same(expected.dropNullValues, got.dropNullValues) }
      .compile
      .toList
      .map(_.combineAll)

  def expectHttpStreamFailure(routes: HttpRoutes[IO], req: Request[IO]): IO[Expectations] =
    jsonStream(routes)(req).attempt
      .map {
        case Left(_)  => success
        case Right(_) => failure("expected a failure")
      }
      .compile
      .toList
      .map(_.combineAll)
}
