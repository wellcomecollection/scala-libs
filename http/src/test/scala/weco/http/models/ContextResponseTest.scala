package weco.http.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions

import java.net.URL

class ContextResponseTest extends AnyFunSpec with Matchers with JsonAssertions {
  it("adds the context URL to a response") {
    case class Shape(sides: Int, color: String)

    val resp = ContextResponse(
      contextUrl = new URL("http://example.org/context.json"),
      result = Shape(sides = 3, color = "blue")
    )

    assertJsonStringsAreEqual(
      toJson(resp).get,
      """
        |{
        |  "@context": "http://example.org/context.json",
        |  "sides": 3,
        |  "color": "blue"
        |}
        |""".stripMargin
    )
  }
}