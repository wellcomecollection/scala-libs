package weco.messaging.sns

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import weco.messaging.{IndividualMessageSender, MessageSender}

import scala.util.{Failure, Success, Try}

class SNSIndividualMessageSender(
  snsClient: SnsClient,
) extends IndividualMessageSender[SNSConfig] {
  override def send(message: String)(
    subject: String,
    destination: SNSConfig): MessageSenderResult = {
    val result = Try {
      snsClient.publish(
        PublishRequest
          .builder()
          .message(message)
          .subject(subject)
          .topicArn(destination.topicArn)
          .build()
      )
    }

    result match {
      case Success(_) => Right(())
      case Failure(e) => Left(SnsErrors.sendErrors(e))
    }
  }
}

class SNSMessageSender(
  snsClient: SnsClient,
  snsConfig: SNSConfig,
  val subject: String
) extends MessageSender[SNSConfig] {
  override protected val underlying: IndividualMessageSender[SNSConfig] =
    new SNSIndividualMessageSender(
      snsClient = snsClient
    )

  override val destination: SNSConfig = snsConfig
}
