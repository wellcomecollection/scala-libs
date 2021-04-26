package weco.http.models

import io.circe.{Encoder, Json}
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import uk.ac.wellcome.json.JsonUtil._

import java.net.URL

case class ContextResponse[T: Encoder](
  @JsonKey("@context") context: String,
  result: T
)

case object ContextResponse {

  def apply[T: Encoder](context: URL, result: T): ContextResponse[T] =
    ContextResponse(context = context.toString, result = result)

  // Flattens the 'result' field into the rest of the object
  implicit def encoder[T: Encoder]: Encoder[ContextResponse[T]] =
    deriveConfiguredEncoder[ContextResponse[T]].mapJson { json =>
      json.asObject
        .flatMap { obj =>
          obj.toMap
            .get("result")
            .flatMap(_.asObject.map(_.toList))
            .map { fields =>
              Json.obj(fields ++ obj.filterKeys(_ != "result").toList: _*)
            }
        }
        .getOrElse(json)
    }
}
