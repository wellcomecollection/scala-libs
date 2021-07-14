package weco.messaging.typesafe

import com.typesafe.config.Config
import software.amazon.awssdk.services.sns.SnsClient
import weco.messaging.sns._
import weco.typesafe.config.builders.EnrichConfig._

object SNSBuilder {
  def buildSNSConfig(config: Config, namespace: String = ""): SNSConfig = {
    val topicArn = config
      .requireString(s"aws.$namespace.sns.topic.arn")

    SNSConfig(topicArn = topicArn)
  }

  def buildSNSClient: SnsClient =
    SnsClient.builder().build()

  def buildSNSIndividualMessageSender: SNSIndividualMessageSender =
    new SNSIndividualMessageSender(
      snsClient = buildSNSClient
    )

  def buildSNSMessageSender(config: Config,
                            namespace: String = "",
                            subject: String): SNSMessageSender =
    new SNSMessageSender(
      snsClient = buildSNSClient,
      snsConfig = buildSNSConfig(config, namespace = namespace),
      subject = subject
    )
}
