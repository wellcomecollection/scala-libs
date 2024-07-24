package weco.messaging.typesafe

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import weco.messaging.sqs.{SQSConfig, SQSStream}
import weco.monitoring.typesafe.CloudWatchBuilder
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object SQSBuilder {
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

  def buildSQSStream[T](config: Config)(implicit actorSystem: ActorSystem,
                                        ec: ExecutionContext): SQSStream[T] =
    new SQSStream[T](
      sqsClient = SqsAsyncClient.builder().build(),
      sqsConfig = buildSQSConfig(config),
      metricsSender = CloudWatchBuilder.buildCloudWatchMetrics(config)
    )
}
