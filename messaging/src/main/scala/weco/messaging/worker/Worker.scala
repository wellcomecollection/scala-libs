package weco.messaging.worker

import grizzled.slf4j.Logging
import weco.messaging.worker.models.{DeterministicFailure, MonitoringProcessorFailure, NonDeterministicFailure, Result, Successful}
import weco.messaging.worker.monitoring.metrics.MetricsRecorder

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action] extends Logging {

  protected val parseWork: Message => Either[Throwable, Work]
  protected val doWork: Work => Future[Result[Summary]]

  implicit val ec: ExecutionContext

  protected val retryAction: Message => Action
  protected val completedAction: Message => Action

  protected val metricsRecorder: MetricsRecorder

  final def processMessage(message: Message): Future[Action] = {
    val startTime = Instant.now()

    val workEither = Try(parseWork(message)) match {
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

  private def process(workEither: Either[Throwable, Work]): Future[Result[Summary]] =
    workEither.fold(
      e => Future.successful(DeterministicFailure[Summary](e)),
      w => doWork(w) recover {
        case e => DeterministicFailure[Summary](e)
      }
    )

  private def log(result: Result[_]): Future[Unit] =
    Future {
      result match {
        case r @ Successful(_)                    => info(r.pretty)
        case r @ NonDeterministicFailure(e, _)    => warn(r.pretty, e)
        case r @ DeterministicFailure(e, _)       => error(r.toString, e)
        case r @ MonitoringProcessorFailure(e, _) => error(r.toString, e)
      }
    }

  private def chooseAction(summary: Result[Summary]): Message => Action =
    summary match {
      case _: NonDeterministicFailure[_] => retryAction
      case _                             => completedAction
    }
}
