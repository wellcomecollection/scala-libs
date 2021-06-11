package weco.http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(implicit system: ActorSystem,
                     val ec: ExecutionContext)
    extends HttpClient with Logging {
  override def singleRequest(request: HttpRequest): Future[HttpResponse] = {
    debug(s"About to send request $request")
    Http().singleRequest(request)
  }
}
