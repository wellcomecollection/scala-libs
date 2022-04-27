package weco.fixtures

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class RandomGeneratorsTest extends AnyFunSpec with Matchers {
  it("generates different results each time by default") {
    val instances = (1 to 5).map(_ => new RandomGenerators {})

    val strings = instances.map(i => i.randomAlphanumeric()).toSet

    strings.size shouldBe 5
  }

  it("generates different results if you set a random seed") {
    val instances = (1 to 5).map(_ =>
      new RandomGenerators {
        override protected lazy val random: Random = new Random(0)
      }
    )

    val strings = instances.map(i => i.randomAlphanumeric()).toSet

    strings.size shouldBe 1
  }
}
