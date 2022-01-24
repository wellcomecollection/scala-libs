package weco.messaging.worker

import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import io.circe.Decoder
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage
import weco.messaging.worker.steps.MessageTransform

trait SnsSqsTransform[Work] extends MessageTransform[SQSMessage, Work] {
  type SQSTransform = SQSMessage => Either[Throwable, Work]

  implicit val wd: Decoder[Work]

  val transform = (message: SQSMessage) => {
    val workResult = for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work

    workResult.toEither
  }
}
