package weco.http

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.ExceptionHandler
import grizzled.slf4j.Logging
import weco.http.json.DisplayJsonUtil
import weco.http.models.{ContextResponse, DisplayError}
import weco.http.monitoring.HttpMetrics

trait WellcomeExceptionHandler
    extends Logging
    with HasContextUrl
    with DisplayJsonUtil {
  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._

  val httpMetrics: HttpMetrics

  implicit val exceptionHandler: ExceptionHandler = buildExceptionHandler()

  private def buildExceptionHandler(): ExceptionHandler =
    ExceptionHandler {
      case err: Exception =>
        logger.error(s"Unexpected exception $err")

        val error = ContextResponse(
          contextUrl = contextUrl,
          DisplayError(statusCode = InternalServerError)
        )

        httpMetrics.sendMetric(InternalServerError)
        complete(InternalServerError -> error)
    }
}
