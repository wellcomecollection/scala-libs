package weco.messaging.worker

import grizzled.slf4j.Logging
import weco.messaging.worker.models.{
  Completed,
  DeterministicFailure,
  MonitoringProcessorFailure,
  NonDeterministicFailure,
  Result,
  Retry,
  Successful,
  WorkCompletion
}
import weco.monitoring.Metrics

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action] extends Logging {
  protected val parseMessage: Message => Try[Work]
  protected val doWork: Work => Future[Result[Summary]]

  type Processed = Future[Action]

  type Completion = WorkCompletion[Message, Summary]
  type MessageAction = Message => Action

  protected val retryAction: MessageAction
  protected val completedAction: MessageAction

  implicit val metrics: Metrics[Future]
  protected val metricsNamespace: String

  implicit val ec: ExecutionContext

  final def processMessage(message: Message): Processed =
    work(message).map(completion)

  private def work(message: Message): Future[Completion] = {
    val startTime = Instant.now()

    for {
      // TODO: DeterministicFailure is the wrong choice here -- it will cause these
      // messages to be marked as "completed", and deleted from the queue.
      //
      // In either case (we can't parse the message, or an unhandled exception in doWork),
      // we should put the messages on a DLQ for further investigation.
      result <- parseMessage(message) match {
        case Failure(e) => Future.successful(DeterministicFailure[Summary](e))

        case Success(work) =>
          doWork(work) recover {
            case e => DeterministicFailure[Summary](e)
          }
      }

      _ = log(result)
      _ <- recordEnd(startTime = startTime, result = result)
    } yield WorkCompletion(message, result)
  }

  private def completion(done: Completion) =
    done match {
      case WorkCompletion(message, response) =>
        response.asInstanceOf[Action] match {
          case _: Retry     => retryAction(message)
          case _: Completed => completedAction(message)
        }
    }

  private def log(result: Result[_]): Unit =
    result match {
      case r @ Successful(_)                    => info(r.pretty)
      case r @ NonDeterministicFailure(e, _)    => warn(r.pretty, e)
      case r @ DeterministicFailure(e, _)       => error(r.toString, e)
      case r @ MonitoringProcessorFailure(e, _) => error(r.toString, e)
    }

  /** Records metrics about the work that's just been completed; in particular the
    * outcome and the duration.
    */
  private def recordEnd(startTime: Instant,
                        result: Result[_]): Future[Result[Unit]] = {
    val futures = Seq(
      metrics.incrementCount(s"$metricsNamespace/${result.name}"),
      metrics
        .recordValue(s"$metricsNamespace/Duration", secondsSince(startTime))
    )

    Future
      .sequence(futures)
      .map(_ => Successful[Unit]())
      .recover { case e => MonitoringProcessorFailure[Unit](e) }
  }

  private def secondsSince(startTime: Instant): Long =
    Duration
      .between(startTime, Instant.now())
      .getSeconds
}
