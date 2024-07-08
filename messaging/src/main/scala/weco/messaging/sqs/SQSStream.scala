package weco.messaging.sqs

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.sqs.MessageAction
import org.apache.pekko.stream.connectors.sqs.scaladsl.{SqsAckSink, SqsSource}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import org.apache.pekko.stream.{ActorAttributes, Supervision}
import org.apache.pekko.{Done, NotUsed}
import grizzled.slf4j.Logging
import io.circe.Decoder
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import weco.json.JsonUtil.fromJson
import weco.json.exceptions.JsonDecodingError
import weco.monitoring.Metrics

import scala.concurrent.Future

// Provides a stream for processing SQS messages.
//
// The main entry point is `foreach` -- callers should create an instance of
// this class, then pass the name of the stream and a processing function
// to foreach.  For example:
//
//      val sqsStream = SQSStream[NotificationMessage]
//
//      def processMessage(message: NotificationMessage): Future[Unit]
//
//      sqsStream.foreach(
//        streamName = "ExampleStream",
//        process = processMessage
//      )
//
class SQSStream[T](
  sqsClient: SqsAsyncClient,
  sqsConfig: SQSConfig,
  metricsSender: Metrics[Future])(implicit val actorSystem: ActorSystem)
    extends Logging {

  implicit val dispatcher = actorSystem.dispatcher

  private val source: Source[Message, NotUsed] =
    SqsSource(sqsConfig.queueUrl)(sqsClient)
  private val sink: Sink[MessageAction, Future[Done]] =
    SqsAckSink.grouped(sqsConfig.queueUrl)(sqsClient)

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T]): Future[Done] =
    runStream(
      streamName = streamName,
      source =>
        source
          .mapAsyncUnordered(parallelism = sqsConfig.parallelism) {
            case (message, t) =>
              debug(s"Processing message ${message.messageId()}")
              process(t).map(_ => message)
        }
    )

  def runGraph(streamName: String)(
    graphBetween: (Source[(Message, T), NotUsed],
                   Sink[Message, Future[Done]]) => RunnableGraph[Future[Done]]
  )(implicit decoder: Decoder[T]): Future[Done] = {
    val metricName = s"${streamName}_ProcessMessage"

    val decodedSource = source
      .map { message =>
        (message, fromJson[T](message.body).get)
      }

    val loggingSink = Flow[Message]
      .map { m =>
        metricsSender.incrementCount(s"${metricName}_success")
        debug(s"Deleting message ${m.messageId()}")
        MessageAction.Delete(m)
      }
      .toMat(sink)(Keep.right)

    graphBetween(decodedSource, loggingSink)
      .withAttributes(ActorAttributes.supervisionStrategy(decider(metricName)))
      .run()
      .map { _ =>
        logger.info("SQSStream finished processing messages.");
        Done
      }
      .recover {
        case err =>
          logger.info(s"SQSStream finished processing with error: $err");
          Done
      }
  }

  def runStream(
    streamName: String,
    modifySource: Source[(Message, T), NotUsed] => Source[Message, NotUsed])(
    implicit decoder: Decoder[T]): Future[Done] =
    runGraph(streamName) { (source, sink) =>
      modifySource(source).toMat(sink)(Keep.right)
    }

  // Defines a "supervision strategy" -- this tells Akka how to react
  // if one of the elements fails.  We want to log the failing message,
  // then drop it and carry on processing the next message.
  //
  // https://doc.akka.io/docs/akka/2.5.6/scala/stream/stream-error.html#supervision-strategies
  //
  private def decider(metricName: String): Supervision.Decider = {
    case e: JsonDecodingError =>
      logException(e)
      metricsSender.incrementCount(s"${metricName}_jsonDecodingFailure")
      Supervision.resume
    case e: Exception =>
      logException(e)
      metricsSender.incrementCount(s"${metricName}_failure")
      Supervision.Resume
    case throwable =>
      logger.warn(s"Received throwable: $throwable. Shutting down.")
      Supervision.Stop
  }

  private def logException(exception: Throwable): Unit =
    exception match {
      case exception: JsonDecodingError =>
        logger.warn(s"JSON decoding error: ${exception.getMessage}")
      case exception: Exception =>
        logger.error(
          s"Unrecognised failure while: ${exception.getMessage}",
          exception)
    }
}
