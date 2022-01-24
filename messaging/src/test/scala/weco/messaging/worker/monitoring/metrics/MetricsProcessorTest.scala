package weco.messaging.worker.monitoring.metrics

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.WorkerFixtures
import weco.monitoring.memory.MemoryMetrics

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits._

class MetricsProcessorTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with WorkerFixtures
    with MetricsFixtures {

  val processor = new MetricsProcessor(
    namespace = s"ns-${randomAlphanumeric()}"
  )

  it("records a success") {
    implicit val metrics = new MemoryMetrics()

    val recorded = processor.recordResult(
      startTime = Instant.now(),
      result = successful(work)
    )

    whenReady(recorded) {
      _ shouldBe (())
    }

    assertMetricCount(
      metrics = metrics,
      metricName = s"${processor.namespace}/Successful",
      expectedCount = 1)
    assertMetricDurations(
      metrics = metrics,
      metricName = s"${processor.namespace}/Duration",
      expectedNumberDurations = 1)
  }

  it("fails if it can't record the metric") {
    implicit val metrics = brokenMemoryMetrics

    val recorded = processor.recordResult(
      startTime = Instant.now(),
      result = successful(work)
    )

    whenReady(recorded.failed) {
      _ shouldBe a[RuntimeException]
    }

    metrics.incrementedCounts shouldBe empty
    metrics.recordedValues shouldBe empty
  }

  it("records a deterministic failure") {
    implicit val metrics = new MemoryMetrics()

    val recorded = processor.recordResult(
      startTime = Instant.now(),
      result = deterministicFailure(work)
    )

    whenReady(recorded) {
      _ shouldBe (())
    }

    assertMetricCount(
      metrics = metrics,
      metricName = s"${processor.namespace}/DeterministicFailure",
      expectedCount = 1)
    assertMetricDurations(
      metrics = metrics,
      metricName = s"${processor.namespace}/Duration",
      expectedNumberDurations = 1)
  }

  it("records a non deterministic failure") {
    implicit val metrics = new MemoryMetrics()

    val recorded = processor.recordResult(
      startTime = Instant.now(),
      result = nonDeterministicFailure(work)
    )

    whenReady(recorded) {
      _ shouldBe (())
    }

    assertMetricCount(
      metrics = metrics,
      metricName = s"${processor.namespace}/NonDeterministicFailure",
      expectedCount = 1)
    assertMetricDurations(
      metrics = metrics,
      metricName = s"${processor.namespace}/Duration",
      expectedNumberDurations = 1)
  }
}
