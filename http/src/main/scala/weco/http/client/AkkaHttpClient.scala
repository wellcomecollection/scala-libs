package weco.http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import weco.Logging

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(implicit system: ActorSystem)
    extends HttpClient
    with Logging {
  implicit val ec: ExecutionContext = system.dispatcher

  override def singleRequest(request: HttpRequest): Future[HttpResponse] = {
    debug(s"About to send request $request")
    Http().singleRequest(request)
  }
}
