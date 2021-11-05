package weco.messaging.sns

import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto._
import io.circe._
import weco.json.JsonUtil._

case class NotificationMessage(
  @JsonKey("Message") body: String
)

case object NotificationMessage {

  // We use these implicits throughout the platform; cache them here to avoid
  // re-deriving them repeatedly.
  implicit val decoder: Decoder[NotificationMessage] = deriveConfiguredDecoder
  implicit val encoder: Encoder[NotificationMessage] = deriveConfiguredEncoder
}
