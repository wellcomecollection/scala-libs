package uk.ac.wellcome.messaging.typesafe
import co.elastic.apm.attach.ElasticApmAttacher
import co.elastic.apm.opentracing.ElasticApmTracer
import com.typesafe.config.Config
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

import scala.jdk.CollectionConverters._

object ApmOpenTracingTracerBuilder {
  def buildCloudwatchMonitoringClient(config: Config): ElasticApmTracer = {
    ElasticApmAttacher.attach(
      Map(
        "application_packages" -> "uk.ac.wellcome",
        "service_name" -> config.requireString(s"apm.tracing.service"),
        "server_urls" -> config.requireString(s"apm.tracing.url"),
        "secret_token" -> config.requireString(s"apm.tracing.secret_token")
      ).asJava)

    new ElasticApmTracer()
  }

}
