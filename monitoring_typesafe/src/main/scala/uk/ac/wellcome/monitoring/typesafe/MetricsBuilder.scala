package uk.ac.wellcome.monitoring.typesafe

import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

import scala.concurrent.duration._
import scala.util.Try

object MetricsBuilder {
  def buildMetricsConfig(config: Config): MetricsConfig = {
    val namespace =
      config.getStringOption("aws.metrics.namespace").getOrElse("")

    val flushInterval = config.getStringOption("aws.metrics.flushInterval")
      .flatMap(s => Try(Duration(s)).toOption)
      .collect { case d: FiniteDuration => d }
      .getOrElse(10 minutes)

    MetricsConfig(
      namespace = namespace,
      flushInterval = flushInterval
    )
  }
}
