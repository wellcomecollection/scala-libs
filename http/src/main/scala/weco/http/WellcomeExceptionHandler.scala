package weco.http

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.ExceptionHandler
import grizzled.slf4j.Logging
import weco.http.json.DisplayJsonUtil
import weco.http.monitoring.HttpMetrics

trait WellcomeExceptionHandler
    extends Logging
    with ErrorDirectives
    with DisplayJsonUtil {

  val httpMetrics: HttpMetrics

  implicit val exceptionHandler: ExceptionHandler = buildExceptionHandler()

  private def buildExceptionHandler(): ExceptionHandler =
    ExceptionHandler {
      case err: Exception =>
        logger.error(s"Unexpected exception $err")

        httpMetrics.sendMetric(InternalServerError)
        internalError(err)
    }
}
