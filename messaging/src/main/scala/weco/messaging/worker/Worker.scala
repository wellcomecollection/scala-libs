package weco.messaging.worker

import weco.messaging.worker.models.{Completed, MonitoringProcessorFailure, Result, Retry, Successful, WorkCompletion}
import weco.messaging.worker.monitoring.metrics.MetricsProcessor
import weco.messaging.worker.steps.{Logger, MessageProcessor, MessageTransform}
import weco.monitoring.Metrics

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait Worker[Message,
             Work,
             InfraServiceMonitoringContext,
             InterServiceMonitoringContext,
             Summary,
             Action]
    extends MessageProcessor[Work, Summary]
    with MessageTransform[Message, Work, InfraServiceMonitoringContext]
    with Logger {

  type Processed = Future[Action]

  type Completion = WorkCompletion[Message, Summary]
  type MessageAction = Message => Action

  protected val retryAction: MessageAction
  protected val completedAction: MessageAction

  val metricsNamespace: String
  implicit val metrics: Metrics[Future]

  implicit val ec: ExecutionContext

  protected lazy val metricsProcessor: MetricsProcessor =
    new MetricsProcessor(metricsNamespace)

  final def processMessage(message: Message): Processed =
    work(message).map(completion)

  private def work(message: Message): Future[Completion] = {
    val startTime = Instant.now()

    for {
      (workEither, _) <- Future.successful(callTransform(message))
      result <- process(workEither)
      _ <- log(result)
      _ <- recordEnd(startTime = startTime, result = result)
    } yield
      WorkCompletion(message, result)
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
  private def recordEnd(startTime: Instant, result: Result[_]): Future[Result[Unit]] =
    metricsProcessor
      .recordResult(result, startTime)
      .map(_ => Successful[Unit]())
      .recover { case e => MonitoringProcessorFailure[Unit](e) }
}
