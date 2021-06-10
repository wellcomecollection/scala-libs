package weco.http.client

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import scala.concurrent.{ExecutionContext, Future}

class MemoryHttpClient(
  responses: Seq[(HttpRequest, HttpResponse)]
)(
  implicit val ec: ExecutionContext
) extends HttpClient {

  private val iterator = responses.toIterator

  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    Future {
      val (nextReq, nextResp) = iterator.next()

      // These checks all amount to "nextReq != request", but the specific
      // checks are meant to make it easier to debug particular issues.

      if (nextReq.uri != request.uri) {
        throw new RuntimeException(
          s"Expected request with URI ${nextReq.uri}, got request with URI ${request.uri}")
      }

      if (nextReq.headers != request.headers) {
        throw new RuntimeException(
          s"Expected request with headers ${nextReq.headers}, got request with headers ${request.headers}")
      }

      if (nextReq != request) {
        throw new RuntimeException(
          s"Expected request $nextReq, but got $request")
      }

      nextResp
    }
}
