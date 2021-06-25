package weco.messaging.typesafe

import co.elastic.apm.attach.ElasticApmAttacher
import co.elastic.apm.opentracing.ElasticApmTracer
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig._

import scala.collection.JavaConverters._

object ApmOpenTracingTracerBuilder {
  def buildCloudwatchMonitoringClient(config: Config): ElasticApmTracer = {
    ElasticApmAttacher.attach(
      Map(
        "application_packages" -> "weco",
        "service_name" -> config.requireString(s"apm.tracing.service"),
        "server_urls" -> config.requireString(s"apm.tracing.url"),
        "secret_token" -> config.requireString(s"apm.tracing.secret_token")
      ).asJava)

    new ElasticApmTracer()
  }

}
