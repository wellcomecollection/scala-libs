package weco.storage.streaming

import java.io.ByteArrayInputStream

import io.circe
import io.circe.Json
import org.apache.commons.io.IOUtils
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators
import weco.json.JsonUtil.{toJson, _}
import weco.storage.JsonEncodingError

class EncoderTest
    extends AnyFunSpec
    with EitherValues
    with Matchers
    with RandomGenerators
    with StreamAssertions {

  import EncoderInstances._

  describe("Encoder") {
    describe("successfully encodes") {
      it("a byte array") {
        val bytes = randomBytes()
        val stream = bytesEncoder.toStream(bytes)

        IOUtils.contentEquals(stream.value, new ByteArrayInputStream(bytes)) shouldBe true
      }

      it("a string") {
        val randomString = randomAlphanumeric()
        val stream = stringEncoder.toStream(randomString)

        assertStreamEquals(
          stream.value,
          randomString,
          expectedLength = randomString.getBytes.length)
      }

      it("some json") {
        val randomString = randomAlphanumeric()
        val randomJson = Json.fromString(randomString)

        val stream = jsonEncoder.toStream(randomJson)

        // len( "{8 chars}" ) ~> 10
        assertStreamEquals(
          stream.value,
          toJson(randomString).get,
          expectedLength = randomJson.noSpaces.getBytes.length)
      }

      it("a type T") {
        case class FilmStar(name: String, age: Int)

        val michael = FilmStar("Michael J. Fox", 57)
        val stream = typeEncoder[FilmStar].toStream(michael)

        // len( {"name":"Michael J. Fox","age":14"} ) ~> 34
        assertStreamEquals(
          stream.value,
          toJson(michael).get,
          expectedLength = 34)
      }
    }

    it("fails to encode if the Circe encoder is broken") {
      case class FilmStar(name: String, age: Int)

      val brokenEncoder = new circe.Encoder[FilmStar] {
        override def apply(a: FilmStar): Json =
          throw new Throwable("boom")
      }

      val michael = FilmStar("Michael J. Fox", 57)
      val stream = typeEncoder[FilmStar](brokenEncoder).toStream(michael)

      stream.left.value shouldBe a[JsonEncodingError]
    }
  }
}
