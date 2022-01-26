package weco.http

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{
  DebuggingDirectives,
  LogEntry,
  LoggingMagnet
}
import akka.http.scaladsl.settings.ServerSettings
import grizzled.slf4j.Logging
import weco.http.errors.WellcomeParsingErrorHandler
import weco.http.models.HTTPServerConfig
import weco.http.monitoring.{HttpMetrics, WellcomeHttpLogger}
import weco.typesafe.Runnable

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class WellcomeHttpApp(
  routes: Route,
  httpServerConfig: HTTPServerConfig,
  val httpMetrics: HttpMetrics,
  val appName: String,
  httpLogger: WellcomeHttpLogger = new WellcomeHttpLogger()
)(
  implicit
  val as: ActorSystem,
  val ec: ExecutionContext
) extends Runnable
    with WellcomeExceptionHandler
    with WellcomeRejectionHandler
    with Logging {

  private val appId = UUID.randomUUID()
  private val appTag = s"$appName/$appId"

  private def createLogLine(logger: LoggingAdapter)(req: HttpRequest)(
    response: Any): Unit = {
    val logLine = httpLogger.createLogLine(req, response)

    LogEntry(s"$appTag - $logLine", Logging.InfoLevel).logTo(logger)
  }

  def run(): Future[_] = {
    val handler: Route = handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        mapResponse { response =>
          httpMetrics.sendMetric(response)

          response
        }(routes)
      }
    }

    val loggedHandler =
      DebuggingDirectives
        .logRequestResult(LoggingMagnet(createLogLine))(handler)

    val settings =
      ServerSettings(as)
        .withParsingErrorHandler(WellcomeParsingErrorHandler.getClass.getName)

    val binding = Http()
      .newServerAt(
        interface = httpServerConfig.host,
        port = httpServerConfig.port
      )
      .withSettings(settings)
      .bindFlow(loggedHandler)

    info(
      s"$appTag - Starting: ${httpServerConfig.host}:${httpServerConfig.port}"
    )

    for {
      server <- binding
      _ = info(
        s"$appTag - Listening: ${httpServerConfig.host}:${httpServerConfig.port}"
      )
      _ <- server.whenTerminated
      _ = info(
        s"$appTag - Terminating: ${httpServerConfig.host}:${httpServerConfig.port}"
      )
    } yield server
  }
}
