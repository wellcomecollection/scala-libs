package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

sealed trait RefreshInterval {
  def toEsValue: String = this match {
    // the default value is taken from the docs
    // ideally we would use `null` but I can't find a way in which elastic4s supports this
    // I'll try add a patch to allow for this, but this gets us closer to reindexing performantly
    // see: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-refresh-interval-setting
    case RefreshInterval.Default      => "1s"
    case RefreshInterval.Off          => "-1"
    case RefreshInterval.On(duration) => s"${duration.toMillis}ms"
  }
}
object RefreshInterval {
  final case object Default extends RefreshInterval
  final case object Off extends RefreshInterval
  final case class On(duration: FiniteDuration) extends RefreshInterval
}

case class IndexConfig(mapping: MappingDefinition,
                       analysis: Analysis,
                       shards: Int = 1,
                       refreshInterval: RefreshInterval =
                         RefreshInterval.Default)

object IndexConfig {
  def empty =
    IndexConfig(
      analysis = Analysis(analyzers = List()),
      mapping = MappingDefinition())

  implicit class IndexConfigOps(indexConfig: IndexConfig) {
    def withRefreshIntervalFromConfig(config: Config): IndexConfig = {
      val isReindexing = config.getBoolean(s"es.is_reindexing")
      val interval =
        if (isReindexing) RefreshInterval.Off else indexConfig.refreshInterval

      indexConfig.copy(refreshInterval = interval)
    }
  }
}
