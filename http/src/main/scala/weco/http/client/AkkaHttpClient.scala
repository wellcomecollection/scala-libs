package weco.http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(implicit system: ActorSystem,
                     val ec: ExecutionContext)
    extends HttpClient {
  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request)
}
