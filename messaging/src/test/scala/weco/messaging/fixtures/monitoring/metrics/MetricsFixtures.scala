package weco.messaging.fixtures.monitoring.metrics

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import weco.fixtures.{RandomGenerators, TestWith}
import weco.messaging.worker.monitoring.metrics.MetricsMonitoringProcessor
import weco.monitoring.memory.MemoryMetrics

import scala.concurrent.{ExecutionContext, Future}

trait MetricsFixtures extends Matchers with RandomGenerators {
  def brokenMemoryMetrics: MemoryMetrics =
    new MemoryMetrics() {
      override def incrementCount(metricName: String): Future[Unit] =
        Future.failed(new RuntimeException("BOOM!"))

      override def recordValue(metricName: String, value: Double): Future[Unit] =
        Future.failed(new RuntimeException("BOOM!"))
    }

  def withMetricsMonitoringProcessor[Work, R](
    namespace: String = s"ns-${randomAlphanumeric()}",
    metrics: MemoryMetrics = new MemoryMetrics
  )(
    testWith: TestWith[(String, MemoryMetrics, MetricsMonitoringProcessor[Work]), R]
  )(
    implicit ec: ExecutionContext
  ): R = {
    val processor = new MetricsMonitoringProcessor[Work](namespace)(metrics, ec)

    testWith((namespace, metrics, processor))
  }

  protected def assertMetricCount(metrics: MemoryMetrics,
                                  metricName: String,
                                  expectedCount: Int): Assertion =
    metrics.incrementedCounts shouldBe (1 to expectedCount).map { _ => metricName}

  protected def assertMetricDurations(metrics: MemoryMetrics,
                                      metricName: String,
                                      expectedNumberDurations: Int): Unit = {
    metrics.recordedValues should have size expectedNumberDurations

    metrics.recordedValues.foreach { value =>
      val (metricName, recordedDuration) = value
      metricName shouldBe metricName
      recordedDuration should be >= 0.0
    }
  }
}
