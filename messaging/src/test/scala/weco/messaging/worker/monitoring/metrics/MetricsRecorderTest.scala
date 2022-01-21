package weco.messaging.worker.monitoring.metrics

import java.time.Instant
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.WorkerFixtures
import weco.messaging.worker.models.{MonitoringProcessorFailure, Successful}

import scala.concurrent.ExecutionContext.Implicits._

class MetricsRecorderTest
    extends AnyFunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with IntegrationPatience
    with WorkerFixtures
    with MetricsFixtures {

  it("records a success metric") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, processor) =>
        val recorded = processor.recordEnd(
          startTime = Instant.now,
          result = successful(work)
        )

        whenReady(recorded) {
          _ shouldBe a[Successful[_]]
        }

        assertMetricCount(
          metrics = metrics,
          metricName = s"$namespace/Successful",
          expectedCount = 1)
        assertMetricDurations(
          metrics = metrics,
          metricName = s"$namespace/Duration",
          expectedNumberDurations = 1)
    }
  }

  it("reports monitoring failure if recording fails") {
    withMetricsMonitoringProcessor[MyWork, Unit](metrics = brokenMemoryMetrics) {
      case (_, metrics, processor) =>
        val recorded = processor.recordEnd(
          startTime = Instant.now,
          result = successful(work)
        )

        whenReady(recorded) {
          _ shouldBe a[MonitoringProcessorFailure[_]]
        }

        metrics.incrementedCounts shouldBe empty
        metrics.recordedValues shouldBe empty
    }
  }

  it("records a terminal failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, processor) =>
        val recorded = processor.recordEnd(
          startTime = Instant.now,
          result = terminalFailure(work)
        )

        whenReady(recorded) {
          _ shouldBe a[Successful[_]]
        }

        assertMetricCount(
          metrics = metrics,
          metricName = s"$namespace/TerminalFailure",
          expectedCount = 1)
        assertMetricDurations(
          metrics = metrics,
          metricName = s"$namespace/Duration",
          expectedNumberDurations = 1)
    }
  }

  it("records a retryable failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, processor) =>
        val recorded = processor.recordEnd(
          startTime = Instant.now,
          result = retryableFailure(work)
        )

        whenReady(recorded) {
          _ shouldBe a[Successful[_]]
        }

        assertMetricCount(
          metrics = metrics,
          metricName = s"$namespace/RetryableFailure",
          expectedCount = 1)
        assertMetricDurations(
          metrics = metrics,
          metricName = s"$namespace/Duration",
          expectedNumberDurations = 1)
    }
  }
}
