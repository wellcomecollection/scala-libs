package weco.messaging.worker

import io.circe.Decoder
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage

trait SnsSqsTransform[Work] {
  implicit val wd: Decoder[Work]

  val parseMessage = (message: SQSMessage) =>
    for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work
}
