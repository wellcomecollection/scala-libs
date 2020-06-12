package uk.ac.wellcome.messaging.worker.monitoring.metrics

import java.time.Instant

import uk.ac.wellcome.messaging.worker.models.{
  MonitoringProcessorFailure,
  Result,
  Successful
}
import uk.ac.wellcome.messaging.worker.steps.MonitoringProcessor

import scala.concurrent.{ExecutionContext, Future}

final class MetricsMonitoringProcessor[Work](val namespace: String)(
  implicit val monitoringClient: MetricsMonitoringClient,
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