package weco.http.models

import io.circe.{Encoder, Json}
import io.circe.syntax._

import java.net.URL

// This is deliberately a regular class, not a case class, so callers are
// forced to use the encoder we've defined below, and not the automatically
// derived encoder for case classes.
class ContextResponse[T: Encoder](
  val contextUrl: URL,
  val result: T
)

case object ContextResponse {
  def apply[T: Encoder](contextUrl: URL, result: T): ContextResponse[T] =
    new ContextResponse(contextUrl = contextUrl, result = result)

  // Add the @context field to the serialised version of T
  implicit def encoder[T: Encoder]: Encoder[ContextResponse[T]] =
    (response: ContextResponse[T]) => {
      response.result.asJson
        .mapObject { json =>
          json.add("@context", Json.fromString(response.contextUrl.toString))
        }
    }
}
