package weco.messaging.worker

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

trait AkkaWorker[Message, Work, Summary, Action]
    extends Worker[Message, Work, Summary, Action] {
  implicit val as: ActorSystem
  override implicit val ec = as.dispatcher

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
