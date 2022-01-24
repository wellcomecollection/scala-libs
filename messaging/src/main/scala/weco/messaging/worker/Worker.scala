package weco.messaging.worker

import weco.messaging.worker.models.{
  Completed,
  MonitoringProcessorFailure,
  Result,
  Retry,
  Successful,
  WorkCompletion
}
import weco.messaging.worker.steps.{Logger, MessageProcessor}
import weco.monitoring.Metrics

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Worker[Message, Work, Summary, Action]
    extends MessageProcessor[Work, Summary]
    with Logger {

  protected val parseMessage: Message => Either[Throwable, Work]

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
    val workEither = doParseMessage(message)

    for {
      result <- process(workEither)
      _ <- log(result)
      _ <- recordEnd(startTime = startTime, result = result)
    } yield WorkCompletion(message, result)
  }

  private def doParseMessage(message: Message): Either[Throwable, Work] =
    Try(parseMessage(message)) match {
      case Failure(e)    => Left(e)
      case Success(work) => work
    }

  private def completion(done: Completion) =
    done match {
      case WorkCompletion(message, response) =>
        response.asInstanceOf[Action] match {
          case _: Retry     => retryAction(message)
          case _: Completed => completedAction(message)
        }
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
