package weco.messaging.typesafe

import com.typesafe.config.Config
import software.amazon.awssdk.services.sns.SnsClient
import weco.config.models.AWSClientConfig
import weco.messaging.sns._
import weco.typesafe.config.builders.AWSClientConfigBuilder
import weco.typesafe.config.builders.EnrichConfig._

object SNSBuilder extends AWSClientConfigBuilder {
  def buildSNSConfig(config: Config, namespace: String = ""): SNSConfig = {
    val topicArn = config
      .requireString(s"aws.$namespace.sns.topic.arn")

    SNSConfig(topicArn = topicArn)
  }

  private def buildSNSClient(awsClientConfig: AWSClientConfig): SnsClient =
    SNSClientFactory.create(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSNSClient(config: Config): SnsClient =
    buildSNSClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sns")
    )

  def buildSNSIndividualMessageSender(
    config: Config): SNSIndividualMessageSender =
    new SNSIndividualMessageSender(
      snsClient = buildSNSClient(config)
    )

  def buildSNSMessageSender(config: Config,
                            namespace: String = "",
                            subject: String): SNSMessageSender =
    new SNSMessageSender(
      snsClient = buildSNSClient(config),
      snsConfig = buildSNSConfig(config, namespace = namespace),
      subject = subject
    )
}
