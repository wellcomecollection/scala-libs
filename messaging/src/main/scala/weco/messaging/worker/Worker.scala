package weco.messaging.worker

import grizzled.slf4j.Logging
import weco.messaging.worker.models.{Completed, DeterministicFailure, MonitoringProcessorFailure, NonDeterministicFailure, Result, Retry, Successful, WorkCompletion}
import weco.messaging.worker.monitoring.metrics.MetricsRecorder

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action] extends Logging {

  protected val parseWork: Message => Either[Throwable, Work]
  protected val doWork: Work => Future[Result[Summary]]

  type Processed = Future[Action]

  type Completion = WorkCompletion[Message, Summary]
  type MessageAction = Message => Action

  implicit val ec: ExecutionContext

  protected val retryAction: MessageAction
  protected val completedAction: MessageAction

  protected val metricsRecorder: MetricsRecorder

  final def processMessage(message: Message): Processed =
    work(message).map(completion)

  private def work(message: Message): Future[Completion] = {
    val startTime = Instant.now()

    val workEither = Try(parseWork(message)) match {
      case Success(value) => value
      case Failure(e)     => Left(e)
    }

    for {
      summary <- process(workEither)
      _ <- log(summary)
      _ <- metricsRecorder.recordEnd(startTime, summary)
    } yield WorkCompletion(message, summary)
  }

  private def process(workEither: Either[Throwable, Work]): Future[Result[Summary]] =
    workEither.fold(
      e => Future.successful(DeterministicFailure[Summary](e)),
      w => doWork(w) recover {
        case e => DeterministicFailure[Summary](e)
      }
    )

  private def completion(done: Completion): Action =
    done match {
      case WorkCompletion(message, response) =>
        response.asInstanceOf[Action] match {
          case _: Retry     => retryAction(message)
          case _: Completed => completedAction(message)
        }
    }

  private def log(result: Result[_]): Future[Unit] =
    Future {
      result match {
        case r @ Successful(_)                    => info(r.pretty)
        case r @ NonDeterministicFailure(e, _)    => warn(r.pretty, e)
        case r @ DeterministicFailure(e, _)       => error(r.toString, e)
        case r @ MonitoringProcessorFailure(e, _) => error(r.toString, e)
      }
    }
}
