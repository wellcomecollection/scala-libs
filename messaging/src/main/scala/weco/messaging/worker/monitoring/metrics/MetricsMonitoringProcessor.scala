package weco.messaging.worker.monitoring.metrics

import java.time.Instant

import weco.messaging.worker.models.{
  MonitoringProcessorFailure,
  Result,
  Successful
}
import weco.messaging.worker.steps.MonitoringProcessor
import weco.monitoring.Metrics

import scala.concurrent.{ExecutionContext, Future}

final class MetricsMonitoringProcessor[Work](val namespace: String)(
  implicit metrics: Metrics[Future],
  val ec: ExecutionContext)
    extends MonitoringProcessor[Work, Instant, Instant]
    with MetricsProcessor {

  override def recordStart(work: Either[Throwable, Work],
                           context: Either[Throwable, Option[Instant]])
    : Future[Either[Throwable, Instant]] =
    Future.successful(Right(Instant.now))

  override def recordEnd[Recorded](
    context: Either[Throwable, Instant],
    result: Result[Recorded]
  ): Future[Result[Unit]] = {

    val monitoring = for {
      _: Unit <- metric(
        result,
        context.getOrElse(throw new Exception(s"context was Left: $context")))
    } yield Successful[Unit]()

    monitoring recover {
      case e => MonitoringProcessorFailure[Unit](e)
    }
  }
}
