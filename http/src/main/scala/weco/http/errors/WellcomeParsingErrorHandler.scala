package weco.http.errors

import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.ParsingErrorHandler
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.settings.ServerSettings
import grizzled.slf4j.Logging
import weco.http.json.DisplayJsonUtil
import weco.http.models.DisplayError

/** Akka's URI parser has a maximum length, configured as `parsing.max-uri-length`.
  *
  * If it gets a request for a longer URI, it returns a response with Content-Type `text/plain`
  * and the response body:
  *
  *     URI length exceeds the configured limit of 2048 characters
  *
  * We occasionally see requests for this sort of URL, which causes errors in our downstream
  * services, because they always expect JSON responses.  This handler wraps those errors in
  * our standard JSON error type.
  *
  * It may also wrap other kinds of parsing error, but I don't know what other parsing errors
  * are possible!
  *
  * See https://github.com/akka/akka-http/pull/3049
  * See https://github.com/wellcomecollection/wellcomecollection.org/issues/7586
  *
  */
object WellcomeParsingErrorHandler
    extends ParsingErrorHandler
    with DisplayJsonUtil
    with Logging {
  override def handle(statusCode: StatusCode,
                      info: ErrorInfo,
                      log: LoggingAdapter,
                      settings: ServerSettings): HttpResponse = {
    warn(s"Illegal request, responding with status '$statusCode': $info")

    val json = toJson(
      DisplayError(statusCode = statusCode, description = info.summary))

    HttpResponse(
      status = statusCode,
      entity = HttpEntity(ContentTypes.`application/json`, json)
    )
  }
}
