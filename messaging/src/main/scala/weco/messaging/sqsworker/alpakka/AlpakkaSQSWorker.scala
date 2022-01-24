package weco.messaging.sqsworker.alpakka

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import grizzled.slf4j.Logging
import io.circe.Decoder
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  SendMessageRequest,
  Message => SQSMessage
}
import weco.json.JsonUtil.fromJson
import weco.messaging.sns.NotificationMessage
import weco.messaging.worker.models.Result
import weco.messaging.worker.AkkaWorker
import weco.monitoring.Metrics

import scala.concurrent.Future

/**
  * Implementation of [[AkkaWorker]] that uses SQS as source and sink.
  * It receives messages from SQS and deletes messages from SQS on successful completion
  */
class AlpakkaSQSWorker[Work, Summary](
  config: AlpakkaSQSWorkerConfig
)(
  val doWork: Work => Future[Result[Summary]]
)(implicit
  val as: ActorSystem,
  val wd: Decoder[Work],
  sc: SqsAsyncClient,
  val metrics: Metrics[Future])
    extends AkkaWorker[SQSMessage, Work, Summary, MessageAction]
    with Logging {
  override protected val metricsNamespace: String =
    config.metricsConfig.namespace

  val parseMessage = (message: SQSMessage) =>
    for {
      notification <- fromJson[NotificationMessage](message.body())
      work <- fromJson[Work](notification.body)
    } yield work

  type SQSAction = SQSMessage => MessageAction

  val parallelism: Int = config.sqsConfig.parallelism
  val source = SqsSource(config.sqsConfig.queueUrl)
  val sink = SqsAckSink.grouped(config.sqsConfig.queueUrl)

  val retryAction: SQSAction = (message: SQSMessage) =>
    MessageAction
      .changeMessageVisibility(message, visibilityTimeout = 0)

  val successfulAction: SQSAction = (message: SQSMessage) =>
    MessageAction.delete(message)

  // We're deliberately quite conservative here -- if a message fails for
  // any reason, we move it to the DLQ and delete it from the original queue.
  // This will flag it for human inspection/intervention.
  //
  // This is balancing several concerns:
  //
  //    - This class is used in the storage service, where we want to be
  //      conservative when things go wrong.  When there is an unexpected failure,
  //      we want to give up immediately and wait for human intervention, rather
  //      than plough on and make things worse.
  //
  //    - Putting the message on a DLQ means it should show up in our DLQ alerting,
  //      and can be easily redriven if the failure was flaky/transient.
  //
  //      This may help us catch issues that would otherwise be lost, e.g. if an app
  //      can't send a failure message to SNS (see https://github.com/wellcomecollection/platform/issues/5419)
  //
  val failureAction: SQSAction = (message: SQSMessage) => {

    // Note: this log includes the full body of the message, so if something goes wrong
    // further down, we at least have that.
    warn(s"Deleting and DLQ'ing failed message $message")

    // Note: this is the best way I can think of to send a message directly to the
    // DLQ; I can't find a way to a better way to modify a message on a queue that
    // doesn't either (1) delete it entirely or (2) make it available to other consumers.
    //
    // The DLQ URL is based on our standard suffix for DLQs; see
    // https://github.com/wellcomecollection/terraform-aws-sqs/blob/a3bb6892b483c9396df594230c96a16524d57c69/queue/main.tf#L22-L24
    //
    // TODO: Make the DLQ URL a configurable parameter.
    sc.sendMessage(
        SendMessageRequest
          .builder()
          .queueUrl(config.sqsConfig.queueUrl + "_dlq")
          .messageBody(message.body())
          .build()
      )
      .get

    MessageAction.delete(message)
  }
}
