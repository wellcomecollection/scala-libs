package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import org.scalatest.funspec.AnyFunSpec
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.DurationInt

class IndexConfigTest extends AnyFunSpec with Matchers {
  val testConfig = IndexConfig(
    analysis = Analysis(analyzers = List()),
    mapping = MappingDefinition(),
    refreshInterval = RefreshInterval.On(30.seconds)
  )

  it("sets the value of RefreshInterval to Off if es.is_reindexing is true") {
    val config = ConfigFactory.load("reindexing.application")
    val newIndexConfig: IndexConfig =
      testConfig.withRefreshIntervalFromConfig(config)

    newIndexConfig.refreshInterval shouldBe RefreshInterval.Off
  }

  it("maintains the value of RefreshInterval if es.is_reindexing is false") {
    val config = ConfigFactory.load("searching.application")
    val newIndexConfig: IndexConfig =
      testConfig.withRefreshIntervalFromConfig(config)

    newIndexConfig.refreshInterval shouldBe testConfig.refreshInterval
  }
}
