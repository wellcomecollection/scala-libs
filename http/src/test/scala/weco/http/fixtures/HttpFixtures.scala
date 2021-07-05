package weco.http.fixtures

import akka.actor.ActorSystem
import org.scalatest.Assertion
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import io.circe.parser._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.fixtures.TestWith
import weco.json.JsonUtil.toJson
import weco.json.utils.JsonAssertions
import weco.http.WellcomeHttpApp
import weco.http.models.HTTPServerConfig
import weco.http.monitoring.{HttpMetricResults, HttpMetrics}
import weco.monitoring.memory.MemoryMetrics

import scala.concurrent.ExecutionContext.Implicits.global

trait HttpFixtures extends Akka with ScalaFutures with Matchers
  with JsonAssertions {

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
    parse(jsonString) match {
      case Right(json) =>
        HttpEntity(ContentTypes.`application/json`, json.noSpaces)

      case Left(err) =>
        throw new IllegalArgumentException(s"Unable to parse JSON ($err):\n$jsonString")
    }

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

    val jsonDescription = description match {
      case Some(desc) => s""" "description": ${toJson(desc).get}, """
      case _ => ""
    }

    val expectedJson =
      s"""
         |{
         |  "errorType": "http",
         |  "httpStatus": ${statusCode.intValue()},
         |  "label": "${statusCode.reason()}",
         |  $jsonDescription
         |  "type": "Error"
         |}
         |""".stripMargin

    withStringEntity(response.entity) {
      assertJsonStringsAreEqual(_, expectedJson)
    }
  }

  def withApp[R](routes: Route, httpMetrics: Option[HttpMetrics] = None, actorSystem: Option[ActorSystem] = None)(testWith: TestWith[WellcomeHttpApp, R]): R =
    withActorSystem { implicit defaultActorSystem =>
      val metricsName = "example.app"

      val defaultHttpMetrics: HttpMetrics = new HttpMetrics(
        name = metricsName,
        metrics = new MemoryMetrics
      )

      val app = new WellcomeHttpApp(
        routes = routes,
        httpMetrics = httpMetrics.getOrElse(defaultHttpMetrics),
        httpServerConfig = httpServerConfigTest,
        appName = metricsName
      )(actorSystem.getOrElse(defaultActorSystem), global)

      app.run()

      testWith(app)
    }
}
