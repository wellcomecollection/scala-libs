package weco.monitoring.typesafe

import akka.stream.Materializer
import com.typesafe.config.Config
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import weco.monitoring.MetricsConfig
import weco.monitoring.cloudwatch.CloudWatchMetrics
import weco.monitoring.typesafe.MetricsBuilder.buildMetricsConfig

import scala.concurrent.ExecutionContext

object CloudWatchBuilder {
  def buildCloudWatchClient: CloudWatchClient =
    CloudWatchClient.builder().build()

  private def buildCloudWatchMetrics(
    cloudWatchClient: CloudWatchClient,
    metricsConfig: MetricsConfig
  )(implicit
    materializer: Materializer,
    ec: ExecutionContext): CloudWatchMetrics =
    new CloudWatchMetrics(
      cloudWatchClient = cloudWatchClient,
      metricsConfig = metricsConfig
    )

  def buildCloudWatchMetrics(config: Config)(
    implicit
    materializer: Materializer,
    ec: ExecutionContext): CloudWatchMetrics =
    buildCloudWatchMetrics(
      cloudWatchClient = CloudWatchBuilder.buildCloudWatchClient,
      metricsConfig = buildMetricsConfig(config)
    )
}
