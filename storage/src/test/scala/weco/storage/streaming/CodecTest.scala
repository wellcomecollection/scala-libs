package weco.storage.streaming

import io.circe.Json
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators
import weco.json.JsonUtil._

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

          val stream = bytesCodec.toStream(bytes).value
          bytesCodec.fromStream(stream).value shouldBe bytes
        }

        it("a string") {
          val randomString = randomAlphanumeric()

          val stream = stringCodec.toStream(randomString).value
          stringCodec.fromStream(stream).value shouldBe randomString
        }

        it("some json") {
          val randomString = randomAlphanumeric()
          val randomJson = Json.fromString(randomString)

          val stream = jsonCodec.toStream(randomJson).value
          jsonCodec.fromStream(stream).value shouldBe randomJson
        }

        it("a type T") {
          case class NamedThing(name: String, value: Int)
          val thing = NamedThing(name = randomAlphanumeric(), value = 5)

          val stream = typeCodec[NamedThing].toStream(thing).value
          typeCodec[NamedThing].fromStream(stream).value shouldBe thing
        }
      }
    }
  }
}
