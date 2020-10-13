package uk.ac.wellcome.messaging.worker.monitoring.metrics.memory

import uk.ac.wellcome.messaging.worker.monitoring.metrics.MetricsMonitoringClient
import uk.ac.wellcome.monitoring.memory.MemoryMetrics

import scala.concurrent.Future

class MemoryMetricsMonitoringClient(val metrics: MemoryMetrics = new MemoryMetrics())
    extends MetricsMonitoringClient {
  override def incrementCount(metricName: String): Future[Unit] =
    metrics.incrementCount(metricName)

  override def recordValue(metricName: String, value: Double): Future[Unit] =
    metrics.recordValue(metricName, value)
}
