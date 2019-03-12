package uk.ac.wellcome.messaging.typesafe

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.typesafe.config.Config
import uk.ac.wellcome.config.models.AWSClientConfig
import uk.ac.wellcome.messaging.sqs.{SQSClientFactory, SQSConfig, SQSStream}
import uk.ac.wellcome.monitoring.typesafe.MetricsBuilder
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object SQSBuilder extends AWSClientConfigBuilder {
  def buildSQSConfig(config: Config, namespace: String = ""): SQSConfig = {
    val queueUrl = config
      .required[String](s"aws.$namespace.sqs.queue.url")
    val parallelism = config
      .getOrElse[String](s"aws.$namespace.sqs.queue.parallelism")(
        default = "10")
    // TODO: revisit to support default and config.getInt, cannot cast String to Integer here
    SQSConfig(
      queueUrl = queueUrl,
      parallelism = Integer.parseInt(parallelism)
    )
  }

  private def buildSQSClient(awsClientConfig: AWSClientConfig): AmazonSQS =
    SQSClientFactory.createSyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSClient(config: Config): AmazonSQS =
    buildSQSClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )

  def buildSQSAsyncClient(awsClientConfig: AWSClientConfig): AmazonSQSAsync =
    SQSClientFactory.createAsyncClient(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildSQSAsyncClient(config: Config): AmazonSQSAsync =
    buildSQSAsyncClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "sqs")
    )

  def buildSQSStream[T](config: Config)(
    implicit
    actorSystem: ActorSystem,
    materializer: ActorMaterializer,
    ec: ExecutionContext): SQSStream[T] =
    new SQSStream[T](
      sqsClient = buildSQSAsyncClient(config),
      sqsConfig = buildSQSConfig(config),
      metricsSender = MetricsBuilder.buildMetricsSender(config)
    )
}
