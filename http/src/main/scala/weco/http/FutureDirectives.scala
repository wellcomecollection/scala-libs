package weco.http

import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait FutureDirectives extends ErrorDirectives {
  def withFuture(future: Future[Route]): Route =
    onComplete(future) {
      case Success(resp) => resp
      case Failure(err)  => internalError(err)
    }
}
