package weco.http.models

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCode, StatusCodes}

// A convenience wrapper for creating JSON HTTP responses in tests.
object JsonHttpResponse {
  def apply(status: StatusCode = StatusCodes.OK, json: String): HttpResponse =
    HttpResponse(
      status = status,
      entity = HttpEntity(ContentTypes.`application/json`, json)
    )
}
