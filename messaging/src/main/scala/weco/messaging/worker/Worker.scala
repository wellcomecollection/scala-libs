package weco.messaging.worker

import grizzled.slf4j.Logging
import weco.messaging.worker.models.{
  MonitoringProcessorFailure,
  Result,
  RetryableFailure,
  Successful,
  TerminalFailure
}
import weco.messaging.worker.monitoring.metrics.MetricsRecorder

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** A Worker:
  *
  *   - receives a `Message` (e.g. a message from an SQS queue)
  *   - extracts an `Input` from the message (e.g. the ID of a record)
  *   - does some processing on the `Input` (e.g. fetching the record and
  *     doing a data transformation)
  *   - gets a `Summary` of the processing received
  *   - applies an `Action` to the message (e.g. removing it from the queue)
  *
  */
trait Worker[Message, Input, Summary, Action] extends Logging {

  // Extracts the `Input` for the `doProcessing` method.
  //
  // e.g. a lot of our applications receive SNS notifications via SQS.  This
  // could unpack the body of the SNS notification from the SQS message.
  protected val parseMessage: Message => Either[Throwable, Input]

  // Do the processing on the `Input`.
  //
  // Ideally this should always return a successful Future.
  protected val doProcessing: Input => Future[Result[Summary]]

  // What to do with a message after it's been processed.
  //
  // e.g. deleting a message from the queue (completedAction) or releasing it
  // to the queue so it can be retried (retryAction)
  protected val retryAction: Message => Action
  protected val completedAction: Message => Action

  implicit val ec: ExecutionContext

  protected val metricsRecorder: MetricsRecorder

  final def processMessage(message: Message): Future[Action] = {
    val startTime = Instant.now()

    val workEither = Try(parseMessage(message)) match {
      case Success(value) => value
      case Failure(e)     => Left(e)
    }

    for {
      summary <- process(workEither)
      _ <- log(summary)
      _ <- metricsRecorder.recordEnd(startTime, summary)
      action = chooseAction(summary)(message)
    } yield action
  }

  private def process(
    workEither: Either[Throwable, Input]): Future[Result[Summary]] =
    workEither.fold(
      e => Future.successful(TerminalFailure[Summary](e)),
      w =>
        doProcessing(w) recover {
          case e => TerminalFailure[Summary](e)
      }
    )

  private def log(result: Result[_]): Future[Unit] =
    Future {
      result match {
        case r @ Successful(_)                    => info(r.pretty)
        case r @ RetryableFailure(e, _)           => warn(r.pretty, e)
        case r @ TerminalFailure(e, _)            => error(r.toString, e)
        case r @ MonitoringProcessorFailure(e, _) => error(r.toString, e)
      }
    }

  private def chooseAction(summary: Result[Summary]): Message => Action =
    summary match {
      case _: RetryableFailure[_] => retryAction
      case _                      => completedAction
    }
}
