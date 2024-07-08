package weco.http

import org.apache.pekko.http.scaladsl.model.StatusCodes.{
  BadRequest,
  InternalServerError,
  NotFound,
  UriTooLong
}
import org.apache.pekko.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators
import weco.http.fixtures.HttpFixtures

class WellcomeHttpAppFeatureTest
    extends AnyFunSpec
    with Matchers
    with HttpFixtures
    with IntegrationPatience
    with RandomGenerators {

  import weco.http.fixtures.ExampleApp._

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
      withApp(brokenExampleApi.routes) { _ =>
        val path = "/example"
        whenGetRequestReady(path) { response =>
          assertIsDisplayError(
            response = response,
            statusCode = InternalServerError
          )
        }
      }
    }

    it("returns a JSON-typed error if you request an overly long URL") {
      withApp(exampleApi.routes) { _ =>
        val path = s"/example?query=${randomAlphanumeric(length = 3000)}"
        whenGetRequestReady(path) { response =>
          assertIsDisplayError(
            response = response,
            description =
              "URI length exceeds the configured limit of 2048 characters",
            statusCode = UriTooLong
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
          string = """
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
          string = """
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

    it(
      "returns a BadRequest with readable error with valid (but incorrect) json") {
      withApp(exampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string = """
              |{
              | "age": 48
              |}
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            description = "Invalid value at .name: Missing required field",
            statusCode = BadRequest
          )
        }
      }
    }

    it("returns a BadRequest with readable error within json") {
      withApp(exampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string = """
              |this.isnt.json: is_it?
              |""".stripMargin
        )

        val path = "/example"

        whenPostRequestReady(path, entity) { response =>
          assertIsDisplayError(
            response = response,
            description =
              "The request content was malformed:\nexpected true got 'this.i...' (line 2, column 1)",
            statusCode = BadRequest
          )
        }
      }
    }

    it("returns an InternalServerError if an exception is thrown") {
      withApp(brokenExampleApi.routes) { _ =>
        val entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string = """
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
