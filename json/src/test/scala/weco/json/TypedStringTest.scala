package weco.json

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions

class TypedStringTest extends AnyFunSpec with Matchers with JsonAssertions {
  class Shape(val underlying: String) extends TypedString[Shape]

  object Shape extends TypedStringOps[Shape] {
    override def apply(name: String): Shape = new Shape(name)
  }

  class Pattern(val underlying: String) extends TypedString[Pattern]

  object Pattern extends TypedStringOps[Pattern] {
    override def apply(name: String): Pattern = new Pattern(name)
  }

  describe("equality") {
    it("an instance is equal to itself") {
      val circle = Shape("circle")

      circle shouldBe circle

      (circle == circle) shouldBe true
      (circle != circle) shouldBe false
    }

    it("equal values are equal") {
      Shape("circle") shouldBe Shape("circle")

      (Shape("circle") == Shape("circle")) shouldBe true
      (Shape("circle") != Shape("circle")) shouldBe false
    }

    it("unequal values are not equal") {
      Shape("circle") should not be Shape("square")

      (Shape("circle") == Shape("square")) shouldBe false
      (Shape("circle") != Shape("square")) shouldBe true
    }

    it("equal values, different types are not equal") {
      Shape("circle") should not be Pattern("circle")

      (Shape("circle") == Pattern("circle")) shouldBe false
      (Shape("circle") != Pattern("circle")) shouldBe true
    }

    it("is not equal to a string") {
      Shape("circle") should not be "circle"

      (Shape("circle") == "circle") shouldBe false
      (Shape("circle") != "circle") shouldBe true
    }
  }

  describe("JSON encoding/decoding") {
    it("round trips to JSON") {
      val square = Shape("square")

      val jsonString = toJson(square).get
      fromJson[Shape](jsonString).get shouldBe square
    }

    it("serialises to a string") {
      val triangle = Shape("triangle")

      assertJsonStringsAreEqual(toJson(triangle).get,
        """
          |"triangle"
          |""".stripMargin)
    }

    it("deserialises from a string") {
      fromJson[Shape](
        """
          |"pentagon"
          |""".stripMargin).get shouldBe Shape("pentagon")
    }
  }
}
