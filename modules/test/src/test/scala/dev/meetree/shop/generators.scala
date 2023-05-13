package dev.meetree.shop

import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.string.ValidBigDecimal
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import squants.market.{ Money, USD }

import java.util.UUID

import dev.meetree.shop.domain.auth.{ EncryptedPassword, Password, User, UserId, UserName }
import dev.meetree.shop.domain.brand.{ Brand, BrandId, BrandName }
import dev.meetree.shop.domain.card.{ Card, CardCVV, CardExpiration, CardName, CardNumber }
import dev.meetree.shop.domain.cart.{ Cart, CartItem, CartTotal, Quantity }
import dev.meetree.shop.domain.category.{ Category, CategoryId, CategoryName }
import dev.meetree.shop.domain.healthcheck.Status
import dev.meetree.shop.domain.item.{ Item, ItemDescription, ItemId, ItemName }
import dev.meetree.shop.domain.order.{ OrderId, PaymentId }
import dev.meetree.shop.domain.param.brand.BrandParam
import dev.meetree.shop.domain.param.item.{ CreateItemParam, ItemDescriptionParam, ItemNameParam, PriceParam }
import dev.meetree.shop.domain.payment.Payment
import dev.meetree.shop.http.auth.user.{ AdminUser, CommonUser }

object generators {

  private def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  private def nesGen[A](f: String => A): Gen[A] =
    for {
      n   <- Gen.chooseNum(21, 40)
      str <- Gen.stringOfN(n, Gen.alphaChar)
    } yield f(str)

  private def sized(size: Int): Gen[Long] = {
    def loop(left: Int, acc: Long): Gen[Long] =
      Gen.oneOf(1 to 9).flatMap { n =>
        if (left > 0) loop(left - 1, acc * 10 + n)
        else acc
      }
    loop(size, 0)
  }

  implicit val userIdArb: Arbitrary[UserId]                       = Arbitrary(idGen(UserId(_)))
  implicit val userNameArb: Arbitrary[UserName]                   = Arbitrary(nesGen(UserName(_)))
  implicit val passwordArb: Arbitrary[Password]                   = Arbitrary(nesGen(Password(_)))
  implicit val encryptedPasswordArb: Arbitrary[EncryptedPassword] =
    Arbitrary(nesGen(EncryptedPassword(_)))

  private val userGen: Gen[User] =
    for {
      i <- arbitrary[UserId]
      n <- arbitrary[UserName]
    } yield User(i, n)

  implicit val adminUserArb: Arbitrary[AdminUser]   = Arbitrary(userGen.map(AdminUser(_)))
  implicit val commonUserArb: Arbitrary[CommonUser] = Arbitrary(userGen.map(CommonUser(_)))

  implicit val brandIdArb: Arbitrary[BrandId] = Arbitrary(idGen(BrandId(_)))
  implicit val brandIdCogen: Cogen[BrandId]   = Cogen[UUID].contramap[BrandId](_.value)
  implicit val brandArb: Arbitrary[Brand]     = Arbitrary(
    for {
      i <- arbitrary[BrandId]
      n <- nesGen(BrandName(_))
    } yield Brand(i, n)
  )

  implicit val brandParamArb: Arbitrary[BrandParam] = Arbitrary(
    arbitrary[NonEmptyString].map(BrandParam(_))
  )

  implicit val categoryIdArb: Arbitrary[CategoryId] = Arbitrary(idGen(CategoryId(_)))
  implicit val categoryIdCogen: Cogen[CategoryId]   = Cogen[UUID].contramap[CategoryId](_.value)
  implicit val categoryArb: Arbitrary[Category]     = Arbitrary(
    for {
      i <- arbitrary[CategoryId]
      n <- nesGen(CategoryName(_))
    } yield Category(i, n)
  )

  implicit val itemIdArb: Arbitrary[ItemId] = Arbitrary(idGen(ItemId(_)))
  implicit val itemArb: Arbitrary[Item]     = Arbitrary(
    for {
      i <- arbitrary[ItemId]
      n <- nesGen(ItemName(_))
      d <- nesGen(ItemDescription(_))
      p <- arbitrary[Money]
      b <- arbitrary[Brand]
      c <- arbitrary[Category]
    } yield Item(i, n, d, p, b, c)
  )

  implicit val createItemParamArb: Arbitrary[CreateItemParam] = Arbitrary(
    for {
      n <- arbitrary[NonEmptyString].map(ItemNameParam(_))
      d <- arbitrary[NonEmptyString].map(ItemDescriptionParam(_))
      p <- arbitrary[String Refined ValidBigDecimal].map(PriceParam(_))
      b <- arbitrary[BrandId]
      c <- arbitrary[CategoryId]
    } yield CreateItemParam(n, d, p, b, c)
  )

  implicit val cardArb: Arbitrary[Card] = Arbitrary(
    for {
      n <- Gen.alphaStr.map(x => CardName(Refined.unsafeApply(x)))
      u <- sized(16).map(x => CardNumber(Refined.unsafeApply(x)))
      x <- sized(4).map(x => CardExpiration(Refined.unsafeApply(x.toString)))
      c <- sized(3).map(x => CardCVV(Refined.unsafeApply(x.toInt)))
    } yield Card(n, u, x, c)
  )

  implicit val moneyArb: Arbitrary[Money]       = Arbitrary(Gen.posNum[Long].map(n => USD(BigDecimal(n))))
  implicit val quantityArb: Arbitrary[Quantity] = Arbitrary(Gen.posNum[Int].map(Quantity(_)))

  private val cartItemGen: Gen[CartItem] =
    for {
      i <- arbitrary[Item]
      q <- arbitrary[Quantity]
    } yield CartItem(i, q)

  implicit val cartItemNelArb: Arbitrary[NonEmptyList[CartItem]] =
    Arbitrary(Gen.nonEmptyListOf(cartItemGen).map(NonEmptyList.fromListUnsafe))
  implicit val cartTotalArb: Arbitrary[CartTotal]                = Arbitrary(
    for {
      i <- arbitrary[NonEmptyList[CartItem]]
      t <- arbitrary[Money]
    } yield CartTotal(i.toList, t)
  )

  private val itemMapGen: Gen[(ItemId, Quantity)] =
    for {
      i <- arbitrary[ItemId]
      q <- arbitrary[Quantity]
    } yield i -> q

  implicit val cartArb: Arbitrary[Cart] = Arbitrary(Gen.nonEmptyMap(itemMapGen).map(Cart(_)))

  implicit val orderIdArb: Arbitrary[OrderId]     = Arbitrary(idGen(OrderId(_)))
  implicit val paymentIdArb: Arbitrary[PaymentId] = Arbitrary(idGen(PaymentId(_)))
  implicit val paymentArb: Arbitrary[Payment]     = Arbitrary(
    for {
      i <- arbitrary[UserId]
      m <- arbitrary[Money]
      c <- arbitrary[Card]
    } yield Payment(i, m, c)
  )

  implicit val statusArb: Arbitrary[Status] = Arbitrary(Gen.oneOf(Status.Okay, Status.Unreachable))
}
