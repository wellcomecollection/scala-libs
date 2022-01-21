package weco.messaging.worker

import weco.messaging.worker.models.{Completed, Retry, WorkCompletion}
import weco.messaging.worker.monitoring.metrics.MetricsRecorder
import weco.messaging.worker.steps.{Logger, MessageProcessor}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action]
    extends MessageProcessor[Work, Summary]
    with Logger {

  protected val parseWork: Message => Either[Throwable, Work]

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

  private def completion(done: Completion): Action =
    done match {
      case WorkCompletion(message, response) =>
        response.asInstanceOf[Action] match {
          case _: Retry     => retryAction(message)
          case _: Completed => completedAction(message)
        }
    }
}
