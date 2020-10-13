package uk.ac.wellcome.messaging.worker.monitoring.metrics.cloudwatch

import uk.ac.wellcome.messaging.worker.monitoring.metrics.MetricsMonitoringClient
import uk.ac.wellcome.monitoring.cloudwatch.CloudWatchMetrics

class CloudwatchMetricsMonitoringClient(val metrics: CloudWatchMetrics)
    extends MetricsMonitoringClient
