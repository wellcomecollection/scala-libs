package weco.messaging.worker

import io.circe.Decoder
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage

trait SnsSqsTransform[Work] {
  type SQSTransform = SQSMessage => Either[Throwable, Work]

  implicit val wd: Decoder[Work]

  val parseMessage = (message: SQSMessage) => {
    val workResult = for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work

    workResult.toEither
  }
}
