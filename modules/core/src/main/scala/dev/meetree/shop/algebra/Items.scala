package dev.meetree.shop.algebra

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.syntax.all._
import fs2.Stream
import skunk.syntax.all._
import skunk.{ Command, Encoder, Query, Session, Void, ~ }

import dev.meetree.shop.domain.ID
import dev.meetree.shop.domain.brand.BrandName
import dev.meetree.shop.domain.item.{ CreateItem, Item, ItemId, UpdateItem }
import dev.meetree.shop.effect.GenUUID

trait Items[F[_]] {
  def findAll: Stream[F, Item]
  def findBy(brand: BrandName): Stream[F, Item]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}

object Items {
  def make[F[_]: GenUUID: Concurrent](
      postgres: Resource[F, Session[F]],
      chunkSize: Int = 1024
  ): Items[F] =
    new Items[F] {
      import ItemSQL._

      def findAll: Stream[F, Item] =
        for {
          session <- Stream.resource(postgres)
          pq      <- Stream.resource(session.prepare(selectAll))
          item    <- pq.stream(Void, chunkSize)
        } yield item

      def findBy(brand: BrandName): Stream[F, Item] =
        for {
          session <- Stream.resource(postgres)
          pq      <- Stream.resource(session.prepare(selectByBrand))
          item    <- pq.stream(brand, chunkSize)
        } yield item

      def findById(itemId: ItemId): F[Option[Item]] =
        postgres.use { session =>
          val prepared = session.prepare(selectById)
          prepared.use(pq => pq.option(itemId))
        }

      def create(item: CreateItem): F[ItemId] =
        postgres.use { session =>
          val prepared = session.prepare(insertItem)
          prepared.use { cmd =>
            for {
              id <- ID.make[F, ItemId]
              _  <- cmd.execute(id ~ item)
            } yield id
          }
        }

      def update(item: UpdateItem): F[Unit] =
        postgres.use { session =>
          val prepared = session.prepare(updateItem)
          prepared.use(cmd => cmd.execute(item).void)
        }
    }
}

object ItemSQL {
  import dev.meetree.shop.sql.codec.{ item, itemId, itemName, itemDescription }
  import dev.meetree.shop.sql.codec.{ brandId, brandName, categoryId, money }

  val selectAll: Query[Void, Item] =
    sql"""
       SELECT i.id, i.name, i.description, i.price,
              b.id, b.name, c.id, c.name
       FROM items AS i
       INNER JOIN brands AS b ON i.brand_id = b.id
       INNER JOIN categories AS c ON i.category_id = c.id
       """.query(item)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
       SELECT i.id, i.name, i.description, i.price,
              b.id, b.name, c.id, c.name
       FROM items AS i
       INNER JOIN brands AS b ON i.brand_id = b.id
       INNER JOIN categories AS c ON i.category_id = c.id
       WHERE b.name LIKE $brandName
       """.query(item)

  val selectById: Query[ItemId, Item] =
    sql"""
       SELECT i.id, i.name, i.description, i.price,
              b.id, b.name, c.id, c.name
       FROM items AS i
       INNER JOIN brands AS b ON i.brand_id = b.id
       INNER JOIN categories AS c ON i.category_id = c.id
       WHERE i.id = $itemId
       """.query(item)

  private val createItem: Encoder[CreateItem] =
    (itemName ~ itemDescription ~ money ~ brandId ~ categoryId).gcontramap[CreateItem]

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
       INSERT INTO items
       VALUES ($itemId, $createItem)
       """.command

  val updateItem: Command[UpdateItem] =
    sql"""
       UPDATE items
       SET price = $money
       WHERE id = $itemId
       """.command
      .contramap(i => i.price ~ i.id)
}
