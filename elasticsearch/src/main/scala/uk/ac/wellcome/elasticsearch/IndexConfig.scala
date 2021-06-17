package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition

import scala.concurrent.duration.FiniteDuration

sealed trait RefreshInterval {
  def toEsValue: String = this match {
    case RefreshInterval.Default => null
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
