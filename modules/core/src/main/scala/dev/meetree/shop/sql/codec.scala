package dev.meetree.shop.sql

import skunk.circe.codec.all.jsonb
import skunk.codec.all.{ numeric, uuid, varchar }
import skunk.~
import squants.market.{ Money, USD }

import dev.meetree.shop.domain.auth.{ EncryptedPassword, UserId, UserName, UserWithPassword }
import dev.meetree.shop.domain.brand.{ Brand, BrandId, BrandName }
import dev.meetree.shop.domain.cart.Quantity
import dev.meetree.shop.domain.category.{ Category, CategoryId, CategoryName }
import dev.meetree.shop.domain.item.{ Item, ItemDescription, ItemId, ItemName }
import dev.meetree.shop.domain.order.{ Order, OrderId, PaymentId }

object codec {
  val money = numeric.imap[Money](USD(_))(_.amount)

  val userId            = uuid.imap[UserId](UserId(_))(_.value)
  val userName          = varchar.imap[UserName](UserName(_))(_.value)
  val encryptedPassword = varchar.imap[EncryptedPassword](EncryptedPassword(_))(_.value)
  val userWithPassword  =
    (userId ~ userName ~ encryptedPassword).gimap[UserWithPassword]

  val brandId   = uuid.imap[BrandId](BrandId(_))(_.value)
  val brandName = varchar.imap[BrandName](BrandName(_))(_.value)
  val brand     =
    (brandId ~ brandName).gimap[Brand]

  val categoryId   = uuid.imap[CategoryId](CategoryId(_))(_.value)
  val categoryName = varchar.imap[CategoryName](CategoryName(_))(_.value)
  val category     =
    (categoryId ~ categoryName).gimap[Category]

  val itemId          = uuid.imap[ItemId](ItemId(_))(_.value)
  val itemName        = varchar.imap[ItemName](ItemName(_))(_.value)
  val itemDescription = varchar.imap[ItemDescription](ItemDescription(_))(_.value)
  val item            =
    (itemId ~ itemName ~ itemDescription ~ money ~ brand ~ category).gmap[Item]

  val orderId    = uuid.imap[OrderId](OrderId(_))(_.value)
  val paymentId  = uuid.imap[PaymentId](PaymentId(_))(_.value)
  val orderItems = jsonb[Map[ItemId, Quantity]]
  val order      =
    (orderId ~ userId ~ paymentId ~ orderItems ~ money).map {
      case oid ~ _ ~ pid ~ items ~ total => Order(oid, pid, items, total)
    }
}
