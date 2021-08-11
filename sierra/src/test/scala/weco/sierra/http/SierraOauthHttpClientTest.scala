package weco.sierra.http

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  OAuth2BearerToken
}
import akka.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class SierraOauthHttpClientTest extends AnyFunSpec with Matchers with Akka with HttpFixtures with IntegrationPatience {
  val credentials = BasicHttpCredentials("username", "password")

  val itemJson: String =
    """
      |{
      |  "id": "1601017",
      |  "updatedDate": "2009-06-15T14:48:00Z",
      |  "createdDate": "2008-05-21T12:47:00Z",
      |  "deleted": false,
      |  "bibIds": [
      |    "1665618"
      |  ],
      |  "location": {
      |    "code": "sicon",
      |    "name": "Closed stores Iconographic"
      |  },
      |  "status": {
      |    "code": "-  ",
      |    "display": "Available"
      |  },
      |  "callNumber": "665618i"
      |}
      |""".stripMargin

  it("fetches a token before making a request") {
    val token = OAuth2BearerToken("dummy_access_token")

    val responses = Seq(
      (
        HttpRequest(
          method = HttpMethods.POST,
          headers = List(Authorization(credentials)),
          uri = Uri("http://sierra:1234/v5/token")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            """
              |{
              |  "access_token": "dummy_access_token",
              |  "token_type": "bearer",
              |  "expires_in": 3600
              |}
              |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token)),
          uri = Uri("http://sierra:1234/v5/items/1601017")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            itemJson
          )
        )
      )
    )

    val underlying = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://sierra:1234/v5")
    }

    withActorSystem { implicit actorSystem =>
      val authClient = new SierraOauthHttpClient(underlying, credentials = credentials)

      val future = authClient.get(path = Path("items/1601017"))

      whenReady(future) { resp =>
        withStringEntity(resp.entity) {
          assertJsonStringsAreEqual(_, itemJson)
        }
      }
    }
  }

  it("fetches a new token when the old token expires") {
    val token1 = OAuth2BearerToken("dummy_access_token1")
    val token2 = OAuth2BearerToken("dummy_access_token2")

    val responses = Seq(
      (
        HttpRequest(
          method = HttpMethods.POST,
          headers = List(Authorization(credentials)),
          uri = Uri("http://sierra:1234/v5/token")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
               |{
               |  "access_token": "${token1.token}",
               |  "token_type": "bearer",
               |  "expires_in": 3
               |}
               |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token1)),
          uri = Uri("http://sierra:1234/v5/items/1601017")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            itemJson
          )
        )
      ),
      (
        HttpRequest(
          method = HttpMethods.POST,
          headers = List(Authorization(credentials)),
          uri = Uri("http://sierra:1234/v5/token")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
               |{
               |  "access_token": "${token2.token}",
               |  "token_type": "bearer",
               |  "expires_in": 3
               |}
               |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token2)),
          uri = Uri("http://sierra:1234/v5/items/1601017")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            itemJson
          )
        )
      )
    )

    val underlying = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://sierra:1234/v5")
    }

    withActorSystem { implicit actorSystem =>
      val authClient = new SierraOauthHttpClient(
        underlying,
        credentials = credentials,
        expiryGracePeriod = 3.seconds
      )

      val future1 = authClient.get(path = Path("items/1601017"))

      whenReady(future1) { resp1 =>
        withStringEntity(resp1.entity) {
          assertJsonStringsAreEqual(_, itemJson)
        }
      }

      Thread.sleep(1000)

      val future2 = authClient.get(path = Path("items/1601017"))

      whenReady(future2) { resp2 =>
        withStringEntity(resp2.entity) {
          assertJsonStringsAreEqual(_, itemJson)
        }
      }
    }
  }
}
