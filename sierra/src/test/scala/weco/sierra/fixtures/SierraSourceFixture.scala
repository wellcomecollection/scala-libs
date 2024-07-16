package weco.sierra.fixtures

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures
import weco.pekko.fixtures.Pekko
import weco.sierra.http.SierraSource

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraSourceFixture extends HttpFixtures with Pekko {
  def withSierraSource[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[SierraSource, R]): R =
    withMaterializer { implicit mat =>
      val httpClient = new MemoryHttpClient(responses) with HttpGet
      with HttpPost {
        override val baseUri: Uri = Uri("http://sierra:1234")
      }

      testWith(new SierraSource(httpClient))
    }
}
