package weco.messaging.worker.monitoring.metrics

import java.time.Instant

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.WorkerFixtures

import scala.concurrent.ExecutionContext.Implicits._

class MetricsMonitoringProcessorTest
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
        val recorded = processor.recordEnd(Right(Instant.now), successful(work))

        whenReady(recorded) { action =>
          shouldBeSuccessful(action)

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
  }

  it("reports monitoring failure if recording fails") {
    withMetricsMonitoringProcessor[MyWork, Unit](metrics = brokenMemoryMetrics) {
      case (_, metrics, processor) =>
        val recorded = processor.recordEnd(Right(Instant.now), successful(work))

        whenReady(recorded) { action =>
          shouldBeMonitoringProcessorFailure(action)

          metrics.incrementedCounts shouldBe empty
          metrics.recordedValues shouldBe empty
        }
    }
  }

  it("records a deterministic failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, processor) =>
        val recorded =
          processor.recordEnd(Right(Instant.now), deterministicFailure(work))

        whenReady(recorded) { action =>
          shouldBeSuccessful(action)

          assertMetricCount(
            metrics = metrics,
            metricName = s"$namespace/DeterministicFailure",
            expectedCount = 1)
          assertMetricDurations(
            metrics = metrics,
            metricName = s"$namespace/Duration",
            expectedNumberDurations = 1)
        }
    }
  }

  it("records a non deterministic failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, processor) =>
        val recorded =
          processor.recordEnd(Right(Instant.now), nonDeterministicFailure(work))

        whenReady(recorded) { action =>
          shouldBeSuccessful(action)

          assertMetricCount(
            metrics = metrics,
            metricName = s"$namespace/NonDeterministicFailure",
            expectedCount = 1)
          assertMetricDurations(
            metrics = metrics,
            metricName = s"$namespace/Duration",
            expectedNumberDurations = 1)
        }
    }
  }
}
