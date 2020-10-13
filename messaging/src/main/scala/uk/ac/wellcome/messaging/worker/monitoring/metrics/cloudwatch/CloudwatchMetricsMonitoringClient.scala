package uk.ac.wellcome.messaging.worker.monitoring.metrics.cloudwatch

import uk.ac.wellcome.messaging.worker.monitoring.metrics.MetricsMonitoringClient
import uk.ac.wellcome.monitoring.cloudwatch.CloudWatchMetrics

import scala.concurrent.Future

class CloudwatchMetricsMonitoringClient(metricsSender: CloudWatchMetrics)
    extends MetricsMonitoringClient {

  override def incrementCount(metricName: String): Future[Unit] =
    metricsSender.incrementCount(metricName)

  override def recordValue(metricName: String, value: Double): Future[Unit] =
    metricsSender.recordValue(metricName, value)
}
