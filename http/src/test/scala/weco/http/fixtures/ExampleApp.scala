package weco.http.fixtures

import akka.http.scaladsl.model.StatusCodes.Accepted
import akka.http.scaladsl.server.Route
import weco.http.FutureDirectives

import java.net.URL
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ExampleApp {
  val contextUrl = new URL("http://api.wellcomecollection.org/example/v1/context.json")

  case class ExampleResource(name: String)

  trait ExampleApi extends FutureDirectives {
    def getTransform(): ExampleResource

    def postTransform(exampleResource: ExampleResource): String

    val routes: Route = concat(
      pathPrefix("example") {
        post {
          entity(as[ExampleResource]) {
            exampleResource: ExampleResource =>
              withFuture {
                Future(complete(Accepted -> postTransform(exampleResource)))
              }
          }
        } ~ get {
          complete(getTransform())
        }
      }
    )

    override def contextUrl: URL = ExampleApp.contextUrl
  }

  val exampleApi = new ExampleApi {
    override def getTransform(): ExampleResource = ExampleResource("hello world")

    override def postTransform(exampleResource: ExampleResource): String = "ok"
  }

  val brokenExampleApi = new ExampleApi {
    override def getTransform(): ExampleResource = throw new Exception("BOOM!!!")

    override def postTransform(exampleResource: ExampleResource): String =
      throw new Exception("BOOM!!!")
  }
}
