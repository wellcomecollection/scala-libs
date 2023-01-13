package weco.elasticsearch

import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition

case class IndexConfig(mapping: MappingDefinition,
                       analysis: Analysis,
                       shards: Int = 1)

object IndexConfig {
  def empty =
    IndexConfig(
      analysis = Analysis(analyzers = List()),
      mapping = MappingDefinition())
}
