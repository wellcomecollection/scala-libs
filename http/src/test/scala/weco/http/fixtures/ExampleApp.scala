package weco.http.fixtures

import org.apache.pekko.http.scaladsl.model.StatusCodes.Accepted
import org.apache.pekko.http.scaladsl.server.Route
import weco.http.FutureDirectives

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ExampleApp {
  case class ExampleResource(name: String)

  trait ExampleApi extends FutureDirectives {
    def getTransform(): ExampleResource

    def postTransform(exampleResource: ExampleResource): String

    val routes: Route = concat(
      pathPrefix("example") {
        post {
          entity(as[ExampleResource]) { exampleResource: ExampleResource =>
            withFuture {
              Future(complete(Accepted -> postTransform(exampleResource)))
            }
          }
        } ~ get {
          complete(getTransform())
        }
      }
    )
  }

  val exampleApi = new ExampleApi {
    override def getTransform(): ExampleResource =
      ExampleResource("hello world")

    override def postTransform(exampleResource: ExampleResource): String = "ok"
  }

  val brokenExampleApi = new ExampleApi {
    override def getTransform(): ExampleResource =
      throw new Exception("BOOM!!!")

    override def postTransform(exampleResource: ExampleResource): String =
      throw new Exception("BOOM!!!")
  }
}
