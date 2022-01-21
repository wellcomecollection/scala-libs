package weco.messaging.sqsworker.alpakka

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import io.circe.Decoder
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage
import weco.messaging.worker.AkkaWorker
import weco.messaging.worker.models.Result
import weco.monitoring.Metrics

import scala.concurrent.Future

/***
  * Implementation of [[AkkaWorker]] that uses SQS as source and sink.
  * It receives messages from SQS and deletes messages from SQS on successful completion
  */
class AlpakkaSQSWorker[Work, Summary](
  config: AlpakkaSQSWorkerConfig,
)(
  val doWork: Work => Future[Result[Summary]]
)(implicit
  val as: ActorSystem,
  val wd: Decoder[Work],
  sc: SqsAsyncClient,
  val metrics: Metrics[Future]
) extends AkkaWorker[SQSMessage, Work, Summary, MessageAction] {

  type SQSAction = SQSMessage => sqs.MessageAction

  override val parseWork = (message: SQSMessage) => {
    val f = for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work

    f.toEither
  }

  override val metricsNamespace: String = config.metricsConfig.namespace

  val parallelism: Int = config.sqsConfig.parallelism
  val source = SqsSource(config.sqsConfig.queueUrl)
  val sink = SqsAckSink.grouped(config.sqsConfig.queueUrl)

  val retryAction: SQSAction = (message: SQSMessage) =>
    MessageAction
      .changeMessageVisibility(message, visibilityTimeout = 0)

  val completedAction: SQSAction = (message: SQSMessage) =>
    MessageAction.delete(message)
}
