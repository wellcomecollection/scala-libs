package weco.http

import java.net.URL

import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, NotFound}
import akka.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.http.fixtures.HttpFixtures

class WellcomeHttpAppFeatureTest
  extends AnyFunSpec
    with Matchers
    with HttpFixtures
    with IntegrationPatience {

  import weco.http.fixtures.ExampleApp._

  override def contextUrl: URL = weco.http.fixtures.ExampleApp.contextUrl

  describe("GET") {
    it("responds to a request") {
      withApp(exampleApi.routes) { _ =>
        val path = "/example"
        whenGetRequestReady(path) {
          _.status shouldBe StatusCodes.OK
        }
      }
    }

    it("serialises an ExampleResource") {
      withApp(exampleApi.routes) { _ =>
        val path = "/example"
        whenGetRequestReady(path) { response =>
          val expectedJson =
            s"""
               |{
               |  "name" : "hello world"
               |}""".stripMargin

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedJson)
          }
        }
      }
    }

    it("returns a NotFound error if you try an unrecognised path") {
      withApp(exampleApi.routes) { _ =>
        whenGetRequestReady("/not-found") { response =>
          assertIsDisplayError(
            response = response,
            description = "The requested resource could not be found.",
            statusCode = NotFound
          )
        }
      }
    }

    it("returns an InternalServerError if an exception is thrown") {
      withApp(brokenGetExampleApi.routes) { _ =>
        val path = "/example"
        whenGetRequestReady(path) { response =>
          assertIsDisplayError(
            response = response,
            statusCode = InternalServerError
          )
        }
      }
    }
  }

  describe("POST") {
    it("responds to a request") {
      withApp(exampleApi.routes) { _ =>

        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string =
            """
              |{
              | "name": "The Count"
              |}
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) {
          _.status shouldBe StatusCodes.Accepted
        }
      }
    }

    it("returns a NotFound error if you try an unrecognised path") {
      withApp(exampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string =
            """
              |{
              | "name": "The Count"
              |}
              |""".stripMargin
        )

        val path = "/not-found"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            description = "The requested resource could not be found.",
            statusCode = NotFound
          )
        }
      }
    }

    it("returns a BadRequest with readable error with valid (but incorrect) json") {
      withApp(exampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string =
            """
              |{
              | "age": 48
              |}
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            description = "Invalid value at .name: required property not supplied.",
            statusCode = BadRequest
          )
        }
      }
    }

    it("returns a BadRequest with readable error within json") {
      withApp(exampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string =
            """
              |this.isnt.json: is_it?
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            description = "The request content was malformed:\nexpected true got 'this.i...' (line 2, column 1)",
            statusCode = BadRequest
          )
        }
      }
    }

    it("returns an InternalServerError if an exception is thrown") {
      withApp(brokenGetExampleApi.routes) { _ =>

        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string =
            """
              |{
              | "name": "gary"
              |}
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            statusCode = InternalServerError
          )
        }
      }
    }
  }
}
