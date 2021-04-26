package weco.http.models

import akka.http.scaladsl.model.StatusCode
import io.swagger.v3.oas.annotations.media.Schema
import io.circe.generic.extras.JsonKey
import io.circe.Encoder
import io.circe.generic.extras.semiauto._
import weco.http.json.DisplayJsonUtil._

@Schema(
  name = "Error"
)
case class DisplayError(
  @Schema(
    description = "The type of error",
    allowableValues = Array("http")
  ) errorType: String = "http",
  @Schema(
    `type` = "Int",
    description = "The HTTP response status code"
  ) httpStatus: Int,
  @Schema(
    description = "The title or other short name of the error"
  ) label: String,
  @Schema(
    `type` = "String",
    description = "The specific error"
  ) description: Option[String] = None,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "Error"
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
