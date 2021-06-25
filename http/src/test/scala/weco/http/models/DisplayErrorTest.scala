package weco.http.models

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.utils.JsonAssertions
import weco.http.json.DisplayJsonUtil._

class DisplayErrorTest extends AnyFunSpec with Matchers with JsonAssertions {
  import DisplayError._

  it("serialises to JSON without a description") {
    val error = DisplayError(statusCode = StatusCodes.InternalServerError)

    assertJsonStringsAreEqual(
      toJson(error),
      s"""
         |{
         |  "errorType": "http",
         |  "httpStatus": 500,
         |  "label": "Internal Server Error",
         |  "type": "Error"
         |}
         |""".stripMargin
    )
  }

  it("serialises to JSON with a description") {
    val error = DisplayError(statusCode = StatusCodes.NotFound, description = "I couldn't find that!")

    assertJsonStringsAreEqual(
      toJson(error),
      s"""
         |{
         |  "errorType": "http",
         |  "httpStatus": 404,
         |  "label": "Not Found",
         |  "description": "I couldn't find that!",
         |  "type": "Error"
         |}
         |""".stripMargin
    )
  }
}
