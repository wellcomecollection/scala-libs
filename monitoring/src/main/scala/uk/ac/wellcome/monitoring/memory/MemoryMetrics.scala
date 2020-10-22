package uk.ac.wellcome.monitoring.memory

import uk.ac.wellcome.monitoring.Metrics

import scala.concurrent.Future

class MemoryMetrics() extends Metrics[Future] {
  var incrementedCounts: Seq[String] = Seq.empty

  override def incrementCount(metricName: String): Future[Unit] = {
    incrementedCounts = incrementedCounts :+ metricName
    Future.successful(())
  }

  var recordedValues: Seq[(String, Double)] = Seq.empty

  override def recordValue(metricName: String, value: Double): Future[Unit] = {
    recordedValues = recordedValues :+ ((metricName, value))
    Future.successful(())
  }
}
