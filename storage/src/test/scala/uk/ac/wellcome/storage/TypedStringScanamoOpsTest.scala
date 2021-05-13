package uk.ac.wellcome.storage

import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scanamo.{DynamoFormat, DynamoValue}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import weco.json.TypedString

class TypedStringScanamoOpsTest extends AnyFunSpec with Matchers with EitherValues {
  class Shape(val underlying: String) extends TypedString[Shape]

  object Shape extends TypedStringScanamoOps[Shape] {
    override def apply(name: String): Shape = new Shape(name)
  }

  describe("Dynamo encoding/decoding") {
    it("round trips to Dynamo") {
      val square = Shape("square")

      val attributeValue = DynamoFormat[Shape].write(square)
      DynamoFormat[Shape].read(attributeValue).value shouldBe square
    }

    it("serialises to a string") {
      val triangle = Shape("triangle")

      DynamoFormat[Shape].write(triangle) shouldBe DynamoValue.fromString("triangle")
    }

    it("deserialises from a string") {
      val av = AttributeValue.builder().s("pentagon").build()

      DynamoFormat[Shape].read(av).value shouldBe Shape("pentagon")
    }
  }
}
