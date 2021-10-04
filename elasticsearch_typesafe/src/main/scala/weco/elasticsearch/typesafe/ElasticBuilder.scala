package weco.elasticsearch.typesafe

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import weco.elasticsearch.ElasticClientBuilder
import weco.typesafe.config.builders.EnrichConfig._

object ElasticBuilder {
  def buildElasticClient(config: Config,
                         namespace: String = ""): ElasticClient = {
    val hostname = config.requireString(s"es.$namespace.host")
    val username = config.requireString(s"es.$namespace.username")
    val password = config.requireString(s"es.$namespace.password")

    val port = config
      .getIntOption(s"es.$namespace.port")
      .getOrElse(9200)
    val protocol = config
      .getStringOption(s"es.$namespace.protocol")
      .getOrElse("http")

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      username = username,
      password = password
    )
  }
}
