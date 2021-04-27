package weco.http.fixtures

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import weco.http.monitoring.HttpMetricResults

trait HttpFixtures extends Matchers {
  def assertMetricSent(
    name: String = "unset",
    metrics: MemoryMetrics,
    result: HttpMetricResults.Value
  ): Assertion =
    metrics.incrementedCounts should contain(
      s"${name}_HttpResponse_$result"
    )
}
