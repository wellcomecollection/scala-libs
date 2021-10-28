package weco.http.monitoring

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.RouteResult.Complete

/** Constructs the log entries for every HTTP request/response pair.
  *
  * We ask callers to specify HTTP headers that they want to be logged, and we don't
  * log anything else.  This is for a couple of reasons:
  *
  *   - shorter logs are easier for humans to read and skim
  *   - higher signal-to-noise ratio in our application logs
  *   - we won't inadvertently log sensitive information in headers, say auth tokens
  *
  */
class WellcomeHttpLogger(
  requestHeaders: Set[String],
  responseHeaders: Set[String]
) {
  def createLogLine(request: HttpRequest, response: Any): String = {
    s"${createRequestLog(request)} / ${createResponseLog(response)}"
  }

  private def createRequestLog(request: HttpRequest): String = {
    val prefix = s"Request: ${request.method.value} ${request.uri.toRelative}"

    val loggedHeaders =
      request.headers
        .filter { h => requestHeaders.contains(h.name) }
        .map { h => s"; ${h.name}=${h.value}"}

    // We only log the entity ("body") if it contains anything interesting.  If the entity
    // is empty (say, on GET requests), it's just noise in the logs.
    val entityLog = request.entity match {
      case h: HttpEntity.Strict if h.contentType == ContentTypes.NoContentType && h.contentLength == 0 => ""
      case _ => s"; ${request.entity}"
    }

    s"$prefix${loggedHeaders.mkString(" ")}$entityLog".trim
  }

  private def createResponseLog(response: Any): String =
    response match {
      case Complete(resp: HttpResponse) =>
        val contentLength = resp.entity.contentLengthOption match {
          case Some(length) => s"$length bytes total"
          case None         => "<unknown>"
        }

        val loggedHeaders =
          resp.headers
            .filter { h => responseHeaders.contains(h.name) }
            .map { h => s"; ${h.name}=${h.value}"}

        s"Response: HTTP ${resp.status}; " +
          s"Content-Type=${resp.entity.contentType}; " +
          s"Content-Length=$contentLength" +
          loggedHeaders.mkString(" ").trim

      case other => s"Response: $other"
    }
}
