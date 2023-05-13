package dev.meetree.shop.domain

import cats.kernel.laws.discipline.MonoidTests
import squants.market.Money
import weaver.FunSuite
import weaver.discipline.Discipline

import dev.meetree.shop.generators.moneyArb

object OrphanSuite extends FunSuite with Discipline {

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)
}
