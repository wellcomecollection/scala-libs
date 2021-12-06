package weco.json.utils

import io.circe.{Json, ParsingFailure}
import io.circe.parser._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}

trait JsonMatchers {
  class JsonMatcher(jsonString1: String) extends BeMatcher[String] {
    override def apply(jsonString2: String): MatchResult = {
      val json1 = parseOrElse(jsonString1)
      val json2 = parseOrElse(jsonString2)

      MatchResult(
        json1 == json2,
        s"JSON strings were not equivalent:\n$json1\n$json2",
        s"JSON strings were equivalent"
      )
    }

    private def parseOrElse(jsonString: String): Json =
      parse(jsonString) match {
        case Right(t) => t
        case Left(err) =>
          println(s"Error trying to parse string <<$jsonString>>")
          throw err
      }
  }

  def equivalentJsonTo(jsonString: String): JsonMatcher =
    new JsonMatcher(jsonString)
}

class JsonMatchersTest extends AnyFunSpec with Matchers with JsonMatchers {
  val pentagon1 =
    s"""
       |{
       |  "name": "pentagon",
       |  "sides": 5
       |}
       |""".stripMargin

  val pentagon2 =
    s"""
       |{
       |  "sides": 5,
       |  "name": "pentagon"
       |}
       |""".stripMargin

  val hexagon =
    s"""
       |{
       |  "sides": 6,
       |  "name": "hexagon"
       |}
       |""".stripMargin

  it("finds JSON strings which are equivalent") {
    pentagon1 shouldBe equivalentJsonTo(pentagon2)
  }

  it("finds JSON strings which are different") {
    pentagon1 should not be equivalentJsonTo(hexagon)
  }

  it("throws if one of the strings is not JSON") {
    intercept[ParsingFailure] {
      "notJson" shouldBe equivalentJsonTo(hexagon)
    }

    intercept[ParsingFailure] {
      hexagon shouldBe equivalentJsonTo("notJson")
    }
  }
}
