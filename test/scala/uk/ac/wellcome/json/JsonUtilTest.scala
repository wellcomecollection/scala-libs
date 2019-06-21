package uk.ac.wellcome.json

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.json.utils.JsonAssertions

class JsonUtilTest extends FunSpec with Matchers with JsonAssertions {
  case class A(id: String, b: B)
  case class B(id: String, c: C)
  case class C(ints: List[Int])

  describe("fromJson") {
    it("successfully parses a JSON string into an instance of a case class") {
      val aId = "a"
      val bId = "b"

      val inputString =
        s"""
           |{
           | "id": "$aId",
           | "b": {
           |   "id": "$bId",
           |   "c": {
           |     "ints": [1,2,3]
           |   }
           | }
           |}
        """.stripMargin

      val triedA = fromJson[A](inputString)
      triedA.isSuccess shouldBe true
      triedA.get shouldBe A(aId, B(bId, C(List(1, 2, 3))))
    }

    it("fails with JsonDecodingError if the json is invalid") {
      val triedA = fromJson[A]("not a valid json string")

      triedA.isFailure shouldBe true
      triedA.failed.get shouldBe a[JsonDecodingError]
    }

    it("fails with JsonDecodingError if the json doesn'tt match case class") {
      val triedA = fromJson[A]("""{"something": "else"}""")

      triedA.isFailure shouldBe true
      triedA.failed.get shouldBe a[JsonDecodingError]
    }
  }

  describe("toJson") {
    it("returns the json string representation of a case class") {
      val a = A(id = "A", b = B(id = "B", c = C(ints = List(1, 2, 3))))

      val triedString = toJson(a)
      triedString.isSuccess shouldBe true
      val expectedString =
        s"""
          |{
          | "id": "${a.id}",
          | "b": {
          |   "id": "${a.b.id}",
          |   "c": {
          |     "ints": [1,2,3]
          |   }
          | }
          |}
        """.stripMargin

      assertJsonStringsAreEqual(triedString.get, expectedString)
    }
  }

  describe("URI conversion") {
    case class Website(title: String, uri: URI)

    it("converts a URI to JSON") {
      val website = Website(
        title = "Wellcome Collection",
        uri = new URI("https://wellcomecollection.org/")
      )

      assertJsonStringsAreEqual(
        toJson(website).get,
        """
          |{
          |  "title": "Wellcome Collection",
          |  "uri": "https://wellcomecollection.org/"
          |}
        """.stripMargin
      )
    }

    it("serialises a JSON string as a URI") {
      val jsonString =
        """
          |{
          |  "title": "JSON",
          |  "uri": "https://json.org/"
          |}
        """.stripMargin

      val website = fromJson[Website](jsonString).get
      website.uri shouldBe new URI("https://json.org/")
    }
  }

  describe("UUID conversion") {
    case class Shipment(name: String, id: UUID)

    it("converts a shipment to JSON") {
      val uuid = uuidFromString("transport")
      val shipment = Shipment(
        name = "Red cars and yellow cars",
        id = uuid
      )

      assertJsonStringsAreEqual(
        toJson(shipment).get,
        s"""
          |{
          |  "name": "Red cars and yellow cars",
          |  "id": "${uuid.toString}"
          |}
        """.stripMargin
      )
    }

    it("serialises a JSON string as a URI") {
      val uuid = uuidFromString("electronics")
      val jsonString =
        s"""
          |{
          |  "name": "Laptops and phones",
          |  "id": "${uuid.toString}"
          |}
        """.stripMargin

      val website = fromJson[Shipment](jsonString).get
      website.id shouldBe uuid
    }

    def uuidFromString(s: String): UUID =
      UUID.nameUUIDFromBytes(s.getBytes())
  }

  describe("Instant conversion") {
    case class Event(name: String, time: Instant)

    it("converts an event to JSON") {
      val now = Instant.now()
      val event = Event(name = "this test", time = now)

      assertJsonStringsAreEqual(
        toJson(event).get,
        s"""
           |{
           |  "name": "this test",
           |  "time": ${now.getEpochSecond}
           |}
        """.stripMargin
      )
    }

    it("serialises a JSON string as an Instant") {
      val jsonString =
        s"""
           |{
           |  "name": "this test",
           |  "id": 1
           |}
        """.stripMargin

      val event = fromJson[Event](jsonString).get
      event.time shouldBe Instant.ofEpochSecond(1)
    }
  }
}
