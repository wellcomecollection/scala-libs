package weco.messaging.sqsworker.alpakka

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import grizzled.slf4j.Logging
import io.circe.Decoder
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import weco.messaging.worker.models.Result
import weco.messaging.worker.monitoring.metrics.MetricsProcessor
import weco.messaging.worker.{AkkaWorker, SnsSqsTransform}
import weco.monitoring.Metrics

import scala.concurrent.Future

/***
  * Implementation of [[AkkaWorker]] that uses SQS as source and sink.
  * It receives messages from SQS and deletes messages from SQS on successful completion
  */
class AlpakkaSQSWorker[Work,
                       InfraServiceMonitoringContext,
                       InterServiceMonitoringContext,
                       Summary](
  config: AlpakkaSQSWorkerConfig,
  val metricsProcessor: MetricsProcessor
)(
  val doWork: Work => Future[Result[Summary]]
)(implicit
  val as: ActorSystem,
  val wd: Decoder[Work],
  sc: SqsAsyncClient,
  val metrics: Metrics[Future])
    extends AkkaWorker[
      SQSMessage,
      Work,
      InfraServiceMonitoringContext,
      InterServiceMonitoringContext,
      Summary,
      MessageAction]
    with SnsSqsTransform[Work, InfraServiceMonitoringContext]
    with Logging {

  type SQSAction = SQSMessage => sqs.MessageAction

  val parallelism: Int = config.sqsConfig.parallelism
  val source = SqsSource(config.sqsConfig.queueUrl)
  val sink = SqsAckSink.grouped(config.sqsConfig.queueUrl)

  val retryAction: SQSAction = (message: SQSMessage) =>
    MessageAction
      .changeMessageVisibility(message, visibilityTimeout = 0)

  val completedAction: SQSAction = (message: SQSMessage) =>
    MessageAction.delete(message)
}
