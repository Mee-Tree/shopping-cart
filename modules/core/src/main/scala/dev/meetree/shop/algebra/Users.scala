package dev.meetree.shop.algebra

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.syntax.all._
import skunk.syntax.all._
import skunk.{ Command, Query, Session, SqlState }

import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.auth.{ EncryptedPassword, UserId, UserName, UserNameInUse, UserWithPassword }
import dev.meetree.shop.effect.GenUUID

trait Users[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName, password: EncryptedPassword): F[UserId]
}

object Users {
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Users[F] = new Users[F] {
    import UserSQL._

    def find(username: UserName): F[Option[UserWithPassword]] =
      postgres.use { session =>
        val prepared = session.prepare(selectUser)
        prepared.use(pq => pq.option(username))
      }

    def create(username: UserName, password: EncryptedPassword): F[UserId] =
      postgres.use { session =>
        val prepared = session.prepare(insertUser)
        prepared
          .use { cmd =>
            for {
              id <- ID.make[F, UserId]
              _  <- cmd.execute(UserWithPassword(id, username, password))
            } yield id
          }
          .recoverWith {
            case SqlState.UniqueViolation(_) => UserNameInUse(username).raiseError
          }
      }
  }
}

private object UserSQL {
  import dev.meetree.shop.sql.codec.{ userWithPassword, userName }

  val selectUser: Query[UserName, UserWithPassword] =
    sql"""
       SELECT * FROM users
       WHERE name = $userName
       """.query(userWithPassword)

  val insertUser: Command[UserWithPassword] =
    sql"""
       INSERT INTO users
       VALUES ($userWithPassword)
       """.command
}
