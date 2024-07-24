package weco.http.models

import org.apache.pekko.http.scaladsl.model.StatusCode
import io.circe.generic.extras.JsonKey
import io.circe.Encoder
import io.circe.generic.extras.semiauto._
import weco.http.json.DisplayJsonUtil._

case class DisplayError(
  errorType: String = "http",
  httpStatus: Int,
  label: String,
  description: Option[String] = None,
  @JsonKey("type") ontologyType: String = "Error"
)

case object DisplayError {
  implicit val encoder: Encoder[DisplayError] = deriveConfiguredEncoder

  def apply(statusCode: StatusCode): DisplayError =
    DisplayError(
      httpStatus = statusCode.intValue(),
      label = statusCode.reason()
    )

  def apply(statusCode: StatusCode, description: String): DisplayError =
    DisplayError(
      httpStatus = statusCode.intValue(),
      label = statusCode.reason(),
      description = Some(description)
    )
}
