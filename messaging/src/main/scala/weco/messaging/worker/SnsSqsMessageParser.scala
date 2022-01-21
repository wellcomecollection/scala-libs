package weco.messaging.worker

import io.circe.Decoder
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage

trait SnsSqsMessageParser[Work] {
  implicit val wd: Decoder[Work]

  val parseWork = (message: SQSMessage) => {
    val f = for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work

    f.toEither
  }
}
