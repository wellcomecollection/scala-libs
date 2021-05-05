package weco.http

import java.net.URL

import akka.http.scaladsl.model.StatusCodes.{Accepted, BadRequest, InternalServerError, NotFound}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import org.scalatest.Assertion
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil.toJson
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import weco.http.fixtures.HttpFixtures
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WellcomeHttpAppFeatureTest
  extends AnyFunSpec
    with Matchers
    with HttpFixtures
    with IntegrationPatience
    with JsonAssertions {

  val exampleApi = new ExampleApi {
    override def getTransform(): ExampleResource = ExampleResource("hello world")

    override def postTransform(exampleResource: ExampleResource): String = "ok"

    override def context: String = contextURLTest
  }

  val brokenGetExampleApi = new ExampleApi {
    override def getTransform(): ExampleResource = throw new Exception("BOOM!!!")

    override def postTransform(exampleResource: ExampleResource): String =
      throw new Exception("BOOM!!!")

    override def context: String = contextURLTest
  }

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

          withStringEntity(response.entity) { actualJson =>
            assertJsonStringsAreEqual(actualJson, expectedJson)
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
            description = "An internal error occurred attempting to process this request!",
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

    // TODO: Expecting more detailed json errors here like in storage service
    it("returns a WATWAT if the request is malformed") {
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
            description = "An internal error occurred attempting to process this request!",
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
            description = "An internal error occurred attempting to process this request!",
            statusCode = InternalServerError
          )
        }
      }
    }
  }

  val contextURLTest = "http://api.wellcomecollection.org/example/v1/context.json"

  def assertIsDisplayError(
                            response: HttpResponse,
                            description: String,
                            statusCode: StatusCode = StatusCodes.BadRequest
                          ): Assertion = {
    response.status shouldBe statusCode
    response.entity.contentType shouldBe ContentTypes.`application/json`

    withStringEntity(response.entity) { jsonResponse =>
      assertJsonStringsAreEqual(
        jsonResponse,
        s"""
           |{
           |  "@context": "$contextURLTest",
           |  "errorType": "http",
           |  "httpStatus": ${statusCode.intValue()},
           |  "label": "${statusCode.reason()}",
           |  "description": ${toJson(description).get},
           |  "type": "Error"
           |}
           |""".stripMargin
      )
    }
  }

  def withApp[R](routes: Route)(testWith: TestWith[WellcomeHttpApp, R]): R =
    withActorSystem { implicit actorSystem =>
      val metricsName = "example.app"

      val httpMetrics = new HttpMetrics(
        name = metricsName,
        metrics = new MemoryMetrics
      )

      val app = new WellcomeHttpApp(
        routes = routes,
        httpMetrics = httpMetrics,
        httpServerConfig = httpServerConfigTest,
        contextURL = new URL(contextURLTest),
        appName = metricsName
      )

      app.run()

      testWith(app)

    }
}

case class ExampleResource(name: String)

trait ExampleApi extends FutureDirectives {
  def getTransform(): ExampleResource

  def postTransform(exampleResource: ExampleResource): String

  val routes: Route = concat(
    pathPrefix("example") {
      post {
        entity(as[ExampleResource]) {
          exampleResource: ExampleResource =>
            withFuture {
              Future(complete(Accepted -> postTransform(exampleResource)))
            }
        }
      } ~ get {
        complete(getTransform())
      }
    }
  )
}