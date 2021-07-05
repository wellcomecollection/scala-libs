package weco.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import grizzled.slf4j.Logging
import weco.http.json.DisplayJsonUtil
import weco.http.models.DisplayError

trait ErrorDirectives
    extends Directives
    with ErrorAccumulatingCirceSupport
    with Logging
    with DisplayJsonUtil {

  def gone(description: String): Route =
    error(
      DisplayError(statusCode = StatusCodes.Gone, description = description)
    )

  def notFound(description: String): Route =
    error(
      DisplayError(statusCode = StatusCodes.NotFound, description = description)
    )

  def invalidRequest(description: String): Route =
    error(
      DisplayError(
        statusCode = StatusCodes.BadRequest,
        description = description)
    )

  private def error(err: DisplayError): Route =
    complete(err.httpStatus -> err)

  def internalError(err: Throwable): Route = {
    logger.error(s"Sending HTTP 500: $err", err)
    error(DisplayError(statusCode = StatusCodes.InternalServerError))
  }
}
