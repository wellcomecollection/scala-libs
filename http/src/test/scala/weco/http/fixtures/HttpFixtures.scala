package weco.http.fixtures

import java.net.URL

import org.scalatest.Assertion
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import weco.http.monitoring.{HttpMetricResults, HttpMetrics}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, RequestEntity, StatusCode}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import io.circe.parser._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil.toJson
import uk.ac.wellcome.json.utils.JsonAssertions
import weco.http.WellcomeHttpApp
import weco.http.fixtures.ExampleApp.context
import weco.http.models.HTTPServerConfig

import scala.concurrent.ExecutionContext

trait HttpFixtures extends Akka with ScalaFutures with Matchers
  with JsonAssertions{

  implicit val ec: ExecutionContext

  private def whenRequestReady[R](
                                   r: HttpRequest
                                 )(testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { response: HttpResponse =>
        testWith(response)
      }
    }

  def whenGetRequestReady[R](path: String)(
    testWith: TestWith[HttpResponse, R]): R = {
    val request = HttpRequest(
      method = GET,
      uri = s"$externalBaseURL$path"
    )

    whenRequestReady(request) { response =>
      testWith(response)
    }
  }

  def createJsonHttpEntityWith(jsonString: String): RequestEntity =
    HttpEntity(
      ContentTypes.`application/json`,
      parse(jsonString).right.get.noSpaces
    )

  def whenPostRequestReady[R](
                               path: String,
                               entity: RequestEntity
                             )(
                               testWith: TestWith[HttpResponse, R]
                             ): R = {
    val request = HttpRequest(
      method = POST,
      uri = s"$externalBaseURL$path",
      entity = entity
    )

    whenRequestReady(request) { response =>
      testWith(response)
    }
  }

  def withStringEntity[R](
                           httpEntity: HttpEntity
                         )(testWith: TestWith[String, R]): R =
    withMaterializer { implicit mat =>
      val value =
        httpEntity.dataBytes.runWith(Sink.fold("") {
          case (acc, byteString) =>
            acc + byteString.utf8String
        })
      whenReady(value) { string =>
        testWith(string)
      }
    }

  private val port = 1234
  private val host = "localhost"
  private val externalBaseURL = s"http://$host:$port"

  val httpServerConfigTest: HTTPServerConfig =
    HTTPServerConfig(host, port, externalBaseURL)

  def assertMetricSent(
                        name: String = "unset",
                        metrics: MemoryMetrics,
                        result: HttpMetricResults.Value
                      ): Assertion =
    metrics.incrementedCounts should contain(
      s"${name}_HttpResponse_$result"
    )

  def assertIsDisplayError(
                            response: HttpResponse,
                            statusCode: StatusCode
                          ): Assertion = {
    assertIsDisplayError(
      response = response,
      description = None,
      statusCode = statusCode
    )
  }

  def assertIsDisplayError(
                            response: HttpResponse,
                            description: String,
                            statusCode: StatusCode
                          ): Assertion = {
    assertIsDisplayError(
      response = response,
      description = Some(description),
      statusCode = statusCode
    )
  }

  def assertIsDisplayError(
                            response: HttpResponse,
                            description: Option[String],
                            statusCode: StatusCode
                          ): Assertion = {
    response.status shouldBe statusCode
    response.entity.contentType shouldBe ContentTypes.`application/json`

    withStringEntity(response.entity) { jsonResponse =>
      assertJsonStringsAreEqual(
        jsonResponse,
        s"""
           |{
           |  "@context": "$context",
           |  "errorType": "http",
           |  "httpStatus": ${statusCode.intValue()},
           |  "label": "${statusCode.reason()}",
           |  "description": ${description.map(d=>toJson(d).get).getOrElse("null")},
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
        contextURL = new URL(context),
        appName = metricsName
      )

      app.run()

      testWith(app)
    }
}