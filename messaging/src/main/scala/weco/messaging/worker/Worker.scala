package weco.messaging.worker

import grizzled.slf4j.Logging
import weco.messaging.worker.models._
import weco.monitoring.Metrics

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action] extends Logging {
  protected val parseMessage: Message => Try[Work]
  protected val doWork: Work => Future[Result[Summary]]

  type MessageAction = Message => Action

  protected val retryAction: MessageAction
  protected val completedAction: MessageAction

  implicit val metrics: Metrics[Future]
  protected val metricsNamespace: String

  implicit val ec: ExecutionContext

  def process(message: Message): Future[Action] = {
    val startTime = Instant.now()

    for {
      // TODO: DeterministicFailure is the wrong choice here -- it will cause these
      // messages to be marked as "completed", and deleted from the queue.
      //
      // In either case (we can't parse the message, or an unhandled exception in doWork),
      // we should put the messages on a DLQ for further investigation.
      result <- parseMessage(message) match {
        case Failure(e) => Future.successful(TerminalFailure[Summary](e))

        case Success(work) =>
          doWork(work) recover {
            case e => TerminalFailure[Summary](e)
          }
      }

      _ = log(result)
      _ <- recordEnd(startTime = startTime, result = result)

      action = chooseAction(result)
    } yield action(message)
  }

  private def chooseAction(result: Result[_]) =
    result match {
      case _: RetryableFailure[_] => retryAction
      case _                             => completedAction
    }

  private def log(result: Result[_]): Unit =
    result match {
      case r @ Successful(_)                    => info(r.pretty)
      case r @ RetryableFailure(e, _)           => warn(r.pretty, e)
      case r @ TerminalFailure(e, _)            => error(r.toString, e)
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
