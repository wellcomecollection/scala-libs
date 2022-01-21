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

final class MetricsMonitoringProcessor(val namespace: String)(
  implicit metrics: Metrics[Future],
  val ec: ExecutionContext)
    extends MonitoringProcessor
    with MetricsProcessor {

  override def recordEnd(startTime: Instant, result: Result[_]): Future[Result[Unit]] = {
    val monitoring = metric(result, startTime)
      .map(_ => Successful[Unit]())

    monitoring recover {
      case e => MonitoringProcessorFailure[Unit](e)
    }
  }
}
