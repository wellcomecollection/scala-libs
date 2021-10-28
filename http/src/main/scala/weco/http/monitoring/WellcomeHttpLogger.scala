package weco.http.monitoring

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpRequest, HttpResponse}
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
  requestHeaders: Set[String] = Set(),
  responseHeaders: Set[String] = Set()
) {
  def createLogLine(request: HttpRequest, response: Any): String = {
    val requestLog = createLogHalf(
      prefix = s"Request: ${request.method.value} ${request.uri.toRelative}",
      headers = request.headers,
      allowedHeaderNames = requestHeaders,
      entity = request.entity
    )

    s"$requestLog / ${createResponseLog(response)}"
  }

  private def createLogHalf(
    prefix: String,
    headers: Seq[HttpHeader],
    allowedHeaderNames: Set[String],
    entity: HttpEntity
  ): String = {
    val loggedHeaders =
      headers
        .filter { h => allowedHeaderNames.contains(h.name) }
        .map { h => s"; ${h.name}=${h.value}"}

    // We only log the entity ("body") if it contains anything interesting.  If the entity
    // is empty (say, on GET requests), it's just noise in the logs.
    val entityLog = entity match {
      case h: HttpEntity.Strict if h.contentType == ContentTypes.NoContentType && h.contentLength == 0 => ""
      case _ => s"; $entity"
    }

    s"$prefix${loggedHeaders.mkString(" ")}$entityLog".trim
  }

  private def createResponseLog(response: Any): String =
    response match {
      case Complete(resp: HttpResponse) =>
        createLogHalf(
          prefix = s"Response: HTTP ${resp.status}",
          headers = resp.headers,
          allowedHeaderNames = responseHeaders,
          entity = resp.entity
        )

      case other => s"Response: $other"
    }
}
