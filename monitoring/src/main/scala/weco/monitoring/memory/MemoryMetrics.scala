package weco.monitoring.memory

import weco.monitoring.Metrics

import scala.concurrent.Future

class MemoryMetrics() extends Metrics[Future] {
  var incrementedCounts: Seq[String] = Seq.empty

  override def incrementCount(metricName: String): Future[Unit] =
    synchronized {
      incrementedCounts = incrementedCounts :+ metricName
      Future.successful(())
    }

  var recordedValues: Seq[(String, Double)] = Seq.empty

  override def recordValue(metricName: String, value: Double): Future[Unit] =
    synchronized {
      recordedValues = recordedValues :+ ((metricName, value))
      Future.successful(())
    }
}
