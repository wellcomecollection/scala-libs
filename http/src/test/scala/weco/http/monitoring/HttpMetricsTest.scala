package weco.http.monitoring

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators
import weco.monitoring.memory.MemoryMetrics

class HttpMetricsTest extends AnyFunSpec with Matchers with RandomGenerators {
  it("records a success") {
    val name = randomAlphanumeric()
    val metrics = new MemoryMetrics
    val httpMetrics = new HttpMetrics(name = name, metrics = metrics)

    httpMetrics.sendMetric(StatusCodes.OK)
    httpMetrics.sendMetric(StatusCodes.Created)

    metrics.incrementedCounts shouldBe Seq(
      s"${name}_HttpResponse_Success",
      s"${name}_HttpResponse_Success"
    )
  }

  it("records a user error") {
    val name = randomAlphanumeric()
    val metrics = new MemoryMetrics
    val httpMetrics = new HttpMetrics(name = name, metrics = metrics)

    httpMetrics.sendMetric(StatusCodes.NotFound)
    httpMetrics.sendMetric(StatusCodes.BadRequest)

    metrics.incrementedCounts shouldBe Seq(
      s"${name}_HttpResponse_UserError",
      s"${name}_HttpResponse_UserError"
    )
  }

  it("records a server error") {
    val name = randomAlphanumeric()
    val metrics = new MemoryMetrics
    val httpMetrics = new HttpMetrics(name = name, metrics = metrics)

    httpMetrics.sendMetric(StatusCodes.InternalServerError)

    metrics.incrementedCounts shouldBe Seq(
      s"${name}_HttpResponse_ServerError"
    )
  }
}
