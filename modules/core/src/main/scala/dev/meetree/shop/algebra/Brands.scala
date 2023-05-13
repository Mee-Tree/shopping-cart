package dev.meetree.shop.algebra

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.syntax.all._
import skunk.syntax.all._
import skunk.{ Codec, Command, Query, Session, Void }

import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.brand.{ Brand, BrandId, BrandName }
import dev.meetree.shop.effect.GenUUID

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(brand: BrandName): F[BrandId]
}

object Brands {
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Brands[F] = new Brands[F] {
    import BrandSQL._

    def findAll: F[List[Brand]] =
      postgres.use(_.execute(selectAll))

    def create(name: BrandName): F[BrandId] =
      postgres.use { session =>
        val prepared = session.prepare(insertBrand)
        prepared.use { cmd =>
          for {
            id <- ID.make[F, BrandId]
            _  <- cmd.execute(Brand(id, name))
          } yield id
        }
      }
  }
}

private object BrandSQL {
  import dev.meetree.shop.sql.codec.{ brandId, brandName }

  val brand: Codec[Brand] =
    (brandId ~ brandName).gimap[Brand]

  val selectAll: Query[Void, Brand] =
    sql"""
       SELECT * FROM brands
       """.query(brand)

  val insertBrand: Command[Brand] =
    sql"""
       INSERT INTO brands
       VALUES ($brand)
       """.command
}
