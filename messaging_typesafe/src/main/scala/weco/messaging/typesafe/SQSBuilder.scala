package weco.messaging.typesafe

import akka.actor.ActorSystem
import com.typesafe.config.Config
import software.amazon.awssdk.services.sqs.{SqsAsyncClient, SqsClient}
import weco.config.models.AWSClientConfig
import weco.messaging.sqs.{SQSClientFactory, SQSConfig, SQSStream}
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.AWSClientConfigBuilder
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object SQSBuilder extends AWSClientConfigBuilder {
  def buildSQSConfig(config: Config, namespace: String = ""): SQSConfig = {
    val queueUrl = config
      .requireString(s"aws.$namespace.sqs.queue.url")
    val parallelism = config
      .getIntOption(s"aws.$namespace.sqs.queue.parallelism")
      .getOrElse(10)

    SQSConfig(
      queueUrl = queueUrl,
      parallelism = parallelism
    )
  }

  private def buildSQSClient(awsClientConfig: AWSClientConfig): SqsClient =
    SQSClientFactory.createSyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSClient(config: Config): SqsClient =
    buildSQSClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )

  def buildSQSAsyncClient(awsClientConfig: AWSClientConfig): SqsAsyncClient =
    SQSClientFactory.createAsyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSAsyncClient(config: Config): SqsAsyncClient =
    buildSQSAsyncClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )

  def buildSQSStream[T](config: Config)(implicit actorSystem: ActorSystem,
                                        ec: ExecutionContext): SQSStream[T] =
    new SQSStream[T](
      sqsClient = buildSQSAsyncClient(config),
      sqsConfig = buildSQSConfig(config),
      metricsSender = CloudWatchBuilder.buildCloudWatchMetrics(config)
    )
}
