package dev.meetree.shop.algebra

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.syntax.all._
import skunk.syntax.all._
import skunk.{ Codec, Command, Query, Session, Void }

import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.category.{ Category, CategoryId, CategoryName }
import dev.meetree.shop.effect.GenUUID

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(category: CategoryName): F[CategoryId]
}

object Categories {
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Categories[F] = new Categories[F] {
    import CategorySQL._

    def findAll: F[List[Category]] =
      postgres.use(_.execute(selectAll))

    def create(name: CategoryName): F[CategoryId] =
      postgres.use { session =>
        val prepared = session.prepare(insertCategory)
        prepared.use { cmd =>
          for {
            id <- ID.make[F, CategoryId]
            _  <- cmd.execute(Category(id, name))
          } yield id
        }
      }
  }
}

private object CategorySQL {
  import dev.meetree.shop.sql.codec.{ categoryId, categoryName }

  val category: Codec[Category] =
    (categoryId ~ categoryName).gimap[Category]

  val selectAll: Query[Void, Category] =
    sql"""
       SELECT * FROM categories
       """.query(category)

  val insertCategory: Command[Category] =
    sql"""
       INSERT INTO categories
       VALUES ($category)
       """.command
}
