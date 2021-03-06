package weco.messaging.worker

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.{Done, NotUsed}
import weco.messaging.worker.steps.MonitoringProcessor

import scala.concurrent.{ExecutionContext, Future}

/**
  * Implementation of [[weco.messaging.worker.Worker]] based on akka streams
  */
trait AkkaWorker[Message,
                 Work,
                 InfraServiceMonitoringContext,
                 InterServiceMonitoringContext,
                 Summary,
                 Action]
    extends Worker[
      Message,
      Work,
      InfraServiceMonitoringContext,
      InterServiceMonitoringContext,
      Summary,
      Action] {

  implicit val as: ActorSystem
  private val ec = as.dispatcher
  protected val monitoringProcessorBuilder: (
    ExecutionContext) => MonitoringProcessor[Work,
                                             InfraServiceMonitoringContext,
                                             InterServiceMonitoringContext]

  override final val monitoringProcessor = monitoringProcessorBuilder(ec)
  type MessageSource = Source[Message, NotUsed]
  type MessageSink = Sink[Action, Future[Done]]

  type ProcessedSource = Source[Action, NotUsed]

  protected val parallelism: Int

  protected val source: MessageSource
  protected val sink: MessageSink

  protected val retryAction: MessageAction
  protected val completedAction: MessageAction

  private def completionSource(parallelism: Int): ProcessedSource =
    source.mapAsyncUnordered(parallelism)(processMessage)

  def start: Future[Done] =
    completionSource(parallelism)
      .toMat(sink)(Keep.right)
      .run()
}
