package uk.ac.wellcome.messaging.worker.monitoring.metrics.memory

import uk.ac.wellcome.messaging.worker.monitoring.metrics.MetricsMonitoringClient
import uk.ac.wellcome.monitoring.memory.MemoryMetrics

class MemoryMetricsMonitoringClient(val metrics: MemoryMetrics = new MemoryMetrics())
    extends MetricsMonitoringClient
