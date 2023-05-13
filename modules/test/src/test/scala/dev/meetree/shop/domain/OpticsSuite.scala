package dev.meetree.shop.domain

import monocle.law.discipline._
import weaver.FunSuite
import weaver.discipline.Discipline

import java.util.UUID

import dev.meetree.shop.domain.brand.BrandId
import dev.meetree.shop.domain.category.CategoryId
import dev.meetree.shop.domain.healthcheck.Status
import dev.meetree.shop.generators.{ brandIdArb, brandIdCogen, categoryIdArb, categoryIdCogen, statusArb }
import dev.meetree.shop.optics.IsUUID

object OpticsSuite extends FunSuite with Discipline {

  checkAll("Iso[Status.bool]", IsoTests(Status.bool))

  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]))
  checkAll("IsUUID[CategoryId]", IsoTests(IsUUID[CategoryId]))
}
