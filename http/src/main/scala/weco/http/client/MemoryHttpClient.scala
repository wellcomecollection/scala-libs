package weco.http.client

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import scala.concurrent.{ExecutionContext, Future}

class MemoryHttpClient(
  responses: Seq[(HttpRequest, HttpResponse)]
)(
  implicit val ec: ExecutionContext
) extends HttpClient {

  val baseUri: Uri = Uri("http://sierra:1234")

  private val iterator = responses.toIterator

  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    Future {
      val (nextReq, nextResp) = iterator.next()

      if (nextReq != request) {
        throw new RuntimeException(
          s"Expected request $nextReq, but got $request")
      }

      nextResp
    }
}
