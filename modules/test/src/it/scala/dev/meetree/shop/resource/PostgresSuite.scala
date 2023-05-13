package dev.meetree.shop.resource

import cats.data.NonEmptyList
import cats.effect.{ IO, Resource }
import cats.implicits._
import natchez.Trace.Implicits.noop
import skunk.syntax.all._
import skunk.{ Command, Session, Void }
import squants.market.Money

import dev.meetree.shop.algebra.{ Brands, Categories, Items, Orders, Users }
import dev.meetree.shop.domain.auth.{ EncryptedPassword, UserName }
import dev.meetree.shop.domain.brand.{ Brand, BrandId }
import dev.meetree.shop.domain.cart.CartItem
import dev.meetree.shop.domain.category.{ Category, CategoryId }
import dev.meetree.shop.domain.item.{ CreateItem, Item }
import dev.meetree.shop.domain.moneyShow
import dev.meetree.shop.domain.order.{ OrderId, PaymentId }
import dev.meetree.shop.generators.{ brandArb, cartItemNelArb, categoryArb, encryptedPasswordArb, itemArb, moneyArb, orderIdArb, paymentIdArb, userNameArb }
import dev.meetree.suite.ResourceSuite

object PostgresSuite extends ResourceSuite {

  val flushTables: List[Command[Void]] =
    List("items", "brands", "categories", "orders", "users").map { table =>
      sql"DELETE FROM #$table".command
    }

  type Res = Resource[IO, Session[IO]]

  override def sharedResource: Resource[IO, Res] =
    Session
      .pooled[IO](
        host = "localhost",
        port = 5432,
        user = "postgres",
        password = Some("password"),
        database = "shop",
        max = 10
      )
      .beforeAll {
        _.use(s => flushTables.traverse_(s.execute))
      }

  test("Brands") { postgres =>
    forall { b: Brand =>
      val bs = Brands.make[IO](postgres)

      for {
        x <- bs.findAll
        _ <- bs.create(b.name)
        y <- bs.findAll
        z <- bs.create(b.name).attempt
      } yield expect.all(
        x.isEmpty,
        y.count(_.name === b.name) === 1,
        z.isLeft
      )
    }
  }

  test("Categories") { postgres =>
    forall { c: Category =>
      val cs = Categories.make[IO](postgres)

      for {
        x <- cs.findAll
        _ <- cs.create(c.name)
        y <- cs.findAll
        z <- cs.create(c.name).attempt
      } yield expect.all(
        x.isEmpty,
        y.count(_.name === c.name) === 1,
        z.isLeft
      )
    }
  }

  test("Items") { postgres =>
    forall { i: Item =>
      def newItem(bid: Option[BrandId], cid: Option[CategoryId]) =
        CreateItem(
          name = i.name,
          description = i.description,
          price = i.price,
          brandId = bid.getOrElse(i.brand.id),
          categoryId = cid.getOrElse(i.category.id)
        )

      val bs = Brands.make[IO](postgres)
      val cs = Categories.make[IO](postgres)
      val is = Items.make[IO](postgres)

      for {
        x <- is.findAll.compile.toList
        _ <- bs.create(i.brand.name)
        d <- bs.findAll.map(_.headOption.map(_.id))
        _ <- cs.create(i.category.name)
        e <- cs.findAll.map(_.headOption.map(_.id))
        _ <- is.create(newItem(d, e))
        y <- is.findAll.compile.toList
      } yield expect.all(
        x.isEmpty,
        y.count(_.name === i.name) === 1
      )
    }
  }

  test("Users") { postgres =>
    forall { (un: UserName, pw: EncryptedPassword) =>
      val us = Users.make[IO](postgres)

      for {
        d <- us.create(un, pw)
        x <- us.find(un)
        z <- us.create(un, pw).attempt
      } yield expect.all(
        x.count(_.id === d) === 1,
        z.isLeft
      )
    }
  }

  test("Orders") { postgres =>
    forall {
      (
          oid: OrderId,
          pid: PaymentId,
          un: UserName,
          pw: EncryptedPassword,
          is: NonEmptyList[CartItem],
          t: Money
      ) =>
        val os = Orders.make[IO](postgres)
        val us = Users.make[IO](postgres)

        for {
          d <- us.create(un, pw)
          x <- os.findBy(d)
          y <- os.get(d, oid)
          i <- os.create(d, pid, is, t)
        } yield expect.all(
          x.isEmpty,
          y.isEmpty,
          i.value.version === 4
        )
    }
  }
}
