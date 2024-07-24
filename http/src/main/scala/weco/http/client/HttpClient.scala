package weco.http.client

import org.apache.pekko.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.Uri.Path.Slash
import org.apache.pekko.http.scaladsl.model.Uri.{Path, Query}
import org.apache.pekko.http.scaladsl.model._
import io.circe.Encoder
import weco.http.json.CirceMarshalling

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  def singleRequest(request: HttpRequest): Future[HttpResponse]
}

trait HttpClientWithBaseUri extends HttpClient {
  val baseUri: Uri

  protected def buildUri(
    path: Path,
    params: Map[String, String]
  ): Uri =
    baseUri
      .withPath(baseUri.path ++ Slash(path))
      .withQuery(Query(params))
}

trait HttpGet extends HttpClientWithBaseUri {
  def get(path: Path,
          params: Map[String, String] = Map.empty): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = buildUri(path, params)
    )

    singleRequest(request)
  }
}

trait HttpPost extends HttpClientWithBaseUri {
  val baseUri: Uri

  implicit val ec: ExecutionContext

  def post[In](path: Path,
               body: Option[In] = None,
               params: Map[String, String] = Map.empty,
               headers: List[HttpHeader] = Nil)(
    implicit encoder: Encoder[In]
  ): Future[HttpResponse] = {
    implicit val um: ToEntityMarshaller[In] = CirceMarshalling.fromEncoder[In]

    for {
      entity <- body match {
        case Some(body) => Marshal(body).to[RequestEntity]
        case None       => Future.successful(HttpEntity.Empty)
      }

      request = HttpRequest(
        HttpMethods.POST,
        uri = buildUri(path, params),
        headers = headers,
        entity = entity
      )

      response <- singleRequest(request)
    } yield response
  }
}
