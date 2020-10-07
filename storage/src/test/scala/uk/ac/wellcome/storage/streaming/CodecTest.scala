package uk.ac.wellcome.storage.streaming

import io.circe.Json
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.json.JsonUtil._

class CodecTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with RandomGenerators {

  import Codec._

  describe("Codec") {
    describe("instances") {
      describe("it is consistent for") {
        it("a byte array") {
          val bytes = randomBytes()

          val stream = bytesCodec.toStream(bytes).right.value
          bytesCodec.fromStream(stream).right.value shouldBe bytes
        }

        it("a string") {
          val randomString = randomAlphanumeric()

          val stream = stringCodec.toStream(randomString).right.value
          stringCodec.fromStream(stream).right.value shouldBe randomString
        }

        it("some json") {
          val randomString = randomAlphanumeric()
          val randomJson = Json.fromString(randomString)

          val stream = jsonCodec.toStream(randomJson).right.value
          jsonCodec.fromStream(stream).right.value shouldBe randomJson
        }

        it("a type T") {
          case class NamedThing(name: String, value: Int)
          val thing = NamedThing(name = randomAlphanumeric(), value = 5)

          val stream = typeCodec[NamedThing].toStream(thing).right.value
          typeCodec[NamedThing].fromStream(stream).right.value shouldBe thing
        }
      }
    }
  }
}
