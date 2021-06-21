package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition

import scala.concurrent.duration.FiniteDuration

sealed trait RefreshInterval {
  def toEsValue: String = this match {
    // the default value is taken from the docs
    // ideally we would use `null` but I can't find a way in which elastic4s supports this
    // I'll try add a patch to allow for this, but this gets us closer to reindexing performantly
    // see: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-refresh-interval-setting
    case RefreshInterval.Default => "1s"
    case RefreshInterval.Off => "-1"
    case RefreshInterval.On(duration) => s"${duration.toMillis}ms"
  }
}
object RefreshInterval {
  final case object Default extends RefreshInterval
  final case object Off extends RefreshInterval
  final case class On(duration: FiniteDuration) extends RefreshInterval
}

trait IndexConfig {
  def mapping: MappingDefinition
  def analysis: Analysis
  def shards: Int = 1
  def refreshInterval: RefreshInterval = RefreshInterval.Default
}

object NoStrictMapping extends IndexConfig {
  val analysis: Analysis = Analysis(analyzers = List())
  val mapping: MappingDefinition = MappingDefinition()
}
