package weco.http.client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import grizzled.slf4j.Logging

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
