package dev.meetree.shop.resource

import cats.effect.kernel.Ref
import cats.effect.{ IO, Resource }
import cats.syntax.all._
import dev.profunktor.auth.jwt.{ JwtAuth, JwtToken, jwtDecode }
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import eu.timepit.refined.auto._
import fs2.Stream
import org.typelevel.log4cats.noop.NoOpLogger
import pdi.jwt.{ JwtAlgorithm, JwtClaim }

import java.util.UUID
import scala.concurrent.duration.DurationInt

import dev.meetree.shop.algebra.{ Auth, Items, ShoppingCart, Users, UsersAuth }
import dev.meetree.shop.auth.{ Crypto, JwtExpire, Tokens }
import dev.meetree.shop.config.types.{ JwtAccessTokenKeyConfig, PasswordSalt, ShoppingCartExpiration, TokenExpiration }
import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.auth.{ EncryptedPassword, InvalidPassword, Password, UserId, UserName, UserNotFound, UserWithPassword }
import dev.meetree.shop.domain.brand.{ Brand, BrandName }
import dev.meetree.shop.domain.cart.{ Cart, Quantity }
import dev.meetree.shop.domain.category.{ Category, CategoryName }
import dev.meetree.shop.domain.item.{ CreateItem, Item, ItemId, UpdateItem }
import dev.meetree.shop.generators.{ itemArb, passwordArb, quantityArb, userIdArb, userNameArb }
import dev.meetree.shop.http.auth.user.UserJwtAuth
import dev.meetree.suite.ResourceSuite

object RedisSuite extends ResourceSuite {

  implicit val logger = NoOpLogger[IO]

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  val CartExp  = ShoppingCartExpiration(30.seconds)
  val TokenExp = TokenExpiration(30.seconds)

  val tokenConfig = JwtAccessTokenKeyConfig("bar")
  val jwtClaim    = JwtClaim("test")
  val userJwtAuth = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  test("Shopping Cart") { redis =>
    forall { (uid: UserId, i1: Item, i2: Item, q1: Quantity, q2: Quantity) =>
      val ref = Ref.of[IO, Map[ItemId, Item]](Map(i1.id -> i1, i2.id -> i2))

      ref.flatMap { ref =>
        val is = new TestItems(ref)
        val sc = ShoppingCart.make[IO](is, redis, CartExp)
        for {
          x <- sc.get(uid)
          _ <- sc.add(uid, i1.id, q1)
          _ <- sc.add(uid, i2.id, q1)
          y <- sc.get(uid)
          _ <- sc.removeItem(uid, i1.id)
          z <- sc.get(uid)
          _ <- sc.update(uid, Cart(Map(i2.id -> q2)))
          w <- sc.get(uid)
          _ <- sc.delete(uid)
          v <- sc.get(uid)
        } yield expect.all(
          x.items.isEmpty,
          y.items.size === 2,
          z.items.size === 1,
          v.items.isEmpty,
          w.items.headOption.fold(false)(_.quantity === q2)
        )
      }
    }
  }

  test("Authentication") { redis =>
    forall { (un1: UserName, un2: UserName, pw: Password) =>
      for {
        ts <- JwtExpire.make[IO].map(Tokens.make[IO](_, tokenConfig, TokenExp))
        c  <- Crypto.make[IO](PasswordSalt("test"))
        a   = Auth.make(TokenExp, ts, new TestUsers(un2), redis, c)
        ua  = UsersAuth.common[IO](redis)
        x  <- ua.findUser(JwtToken("invalid"))(jwtClaim)
        y  <- a.login(un1, pw).attempt // UserNotFound
        j  <- a.newUser(un1, pw)
        e  <- jwtDecode[IO](j, userJwtAuth.value).attempt
        k  <- a.login(un2, pw).attempt // InvalidPassword
        w  <- ua.findUser(j)(jwtClaim)
        s  <- redis.get(j.value)
        _  <- a.logout(j, un1)
        z  <- redis.get(j.value)
      } yield expect.all(
        x.isEmpty,
        y == Left(UserNotFound(un1)),
        e.isRight,
        k == Left(InvalidPassword(un2)),
        w.fold(false)(_.value.name === un1),
        s.nonEmpty,
        z.isEmpty
      )
    }
  }
}

protected class TestUsers(un: UserName) extends Users[IO] {
  def find(username: UserName): IO[Option[UserWithPassword]]              =
    (username === un)
      .guard[Option]
      .as(UserWithPassword(UserId(UUID.randomUUID), un, EncryptedPassword("foo")))
      .pure[IO]
  def create(username: UserName, password: EncryptedPassword): IO[UserId] =
    ID.make[IO, UserId]
}

protected class TestItems(ref: Ref[IO, Map[ItemId, Item]]) extends Items[IO] {
  def findAll: Stream[IO, Item]                  =
    Stream.evals(ref.get.map(_.values.toList))
  def findBy(brand: BrandName): Stream[IO, Item] =
    Stream.evals(ref.get.map(_.values.filter(_.brand.name === brand).toList))
  def findById(itemId: ItemId): IO[Option[Item]] =
    ref.get.map(_.get(itemId))
  def create(item: CreateItem): IO[ItemId]       =
    ID.make[IO, ItemId].flatTap { id =>
      val brand    = Brand(item.brandId, BrandName("foo"))
      val category = Category(item.categoryId, CategoryName("foo"))
      val newItem  = Item(id, item.name, item.description, item.price, brand, category)
      ref.update(_.updated(id, newItem))
    }
  def update(item: UpdateItem): IO[Unit]         =
    ref.update(x => x.get(item.id).fold(x)(i => x.updated(item.id, i.copy(price = item.price))))
}
