package weco.monitoring.typesafe

import org.apache.pekko.stream.Materializer
import com.typesafe.config.Config
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import weco.monitoring.MetricsConfig
import weco.monitoring.cloudwatch.CloudWatchMetrics
import weco.monitoring.typesafe.MetricsBuilder.buildMetricsConfig

import scala.concurrent.ExecutionContext

object CloudWatchBuilder {
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
      cloudWatchClient = CloudWatchClient.builder().build(),
      metricsConfig = buildMetricsConfig(config)
    )
}
