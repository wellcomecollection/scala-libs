package weco.http.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class MemoryHttpClientTest extends AnyFunSpec with Matchers with ScalaFutures {
  it("treats request entities as similar if they have the same JSON") {
    val client = new MemoryHttpClient(
      responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri("http://example.net/shape"),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                 |{
                 |  "color": "red",
                 |  "sides": 4
                 |}
                 |""".stripMargin
            )
          ),
          HttpResponse(status = StatusCodes.Created)
        )
      )
    )

    val future = client.singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("http://example.net/shape"),
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          """
             |{"color": "red","sides": 4}
             |""".stripMargin
        )
      )
    )

    whenReady(future) {
      _.status shouldBe StatusCodes.Created
    }
  }
}
