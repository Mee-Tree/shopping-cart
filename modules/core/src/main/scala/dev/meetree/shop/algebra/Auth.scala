package dev.meetree.shop.algebra

import cats.data.OptionT
import cats.syntax.all._
import cats.{ Applicative, Monad, MonadThrow }
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import io.circe.syntax._
import pdi.jwt.JwtClaim

import dev.meetree.shop.auth.{ Crypto, Tokens }
import dev.meetree.shop.config.types.TokenExpiration
import dev.meetree.shop.domain.auth.{ InvalidPassword, Password, User, UserName, UserNameInUse, UserNotFound }
import dev.meetree.shop.domain.{ jwtEq, jwtShow }
import dev.meetree.shop.http.auth.user.{ AdminUser, CommonUser }

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

object Auth {
  def make[F[_]: MonadThrow](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String],
      crypto: Crypto
  ): Auth[F] = new Auth[F] {
    private val TokenExpiration = tokenExpiration.value

    private def newToken(user: User): F[JwtToken] =
      for {
        token <- tokens.create
        uJson  = user.asJson.noSpaces
        _     <- redis.setEx(token.show, uJson, TokenExpiration)
        _     <- redis.setEx(user.name.show, token.show, TokenExpiration)
      } yield token

    def newUser(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap {
        case Some(_) => UserNameInUse(username).raiseError
        case None    =>
          for {
            id    <- users.create(username, crypto.encrypt(password))
            token <- newToken(User(id, username))
          } yield token
      }

    def login(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap {
        case None                                                     =>
          UserNotFound(username).raiseError
        case Some(user) if user.password =!= crypto.encrypt(password) =>
          InvalidPassword(username).raiseError
        case Some(user)                                               =>
          redis.get(username.show).flatMap {
            case Some(t) => JwtToken(t).pure[F]
            case None    => newToken(user.withoutPassword)
          }
      }

    def logout(token: JwtToken, username: UserName): F[Unit] =
      redis.del(token.show) *> redis.del(username.show).void
  }
}

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

object UsersAuth {
  def common[F[_]: Monad](
      redis: RedisCommands[F, String, String]
  ): UsersAuth[F, CommonUser] = new UsersAuth[F, CommonUser] {

    def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
      (for {
        uJson <- OptionT(redis.get(token.value))
        user  <- OptionT.fromOption(decode[User](uJson).toOption)
      } yield CommonUser(user)).value
  }

  def admin[F[_]: Applicative](
      adminToken: JwtToken,
      adminUser: AdminUser
  ): UsersAuth[F, AdminUser] = new UsersAuth[F, AdminUser] {

    def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
      (token === adminToken)
        .guard[Option]
        .as(adminUser)
        .pure[F]
  }
}
