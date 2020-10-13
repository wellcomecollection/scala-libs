package uk.ac.wellcome.messaging.worker.monitoring.metrics

import uk.ac.wellcome.monitoring.Metrics

import scala.concurrent.Future

trait MetricsMonitoringClient {
  val metrics: Metrics[Future]

  def incrementCount(metricName: String): Future[Unit] =
    metrics.incrementCount(metricName)

  def recordValue(metricName: String, value: Double): Future[Unit] =
    metrics.recordValue(metricName, value)
}
