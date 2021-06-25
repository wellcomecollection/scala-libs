package weco.messaging.worker

import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import io.circe.Decoder
import weco.json.JsonUtil._
import weco.messaging.sns.NotificationMessage
import weco.messaging.worker.steps.MessageTransform

trait SnsSqsTransform[Work, MonitoringContext]
    extends MessageTransform[SQSMessage, Work, MonitoringContext] {

  type SQSTransform = SQSMessage => Transformed

  implicit val nd = implicitly[Decoder[NotificationMessage]]
  implicit val wd: Decoder[Work]

  val transform: SQSTransform = (message: SQSMessage) => {
    val f = for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work
    (f.toEither, Right(None))
  }
}
