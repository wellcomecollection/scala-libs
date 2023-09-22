package weco.elasticsearch.typesafe

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import weco.elasticsearch.ElasticClientBuilder
import weco.typesafe.config.builders.EnrichConfig._

object ElasticBuilder {
  def buildElasticClient(config: Config,
                         namespace: String = ""): ElasticClient = {
    val hostname = config.requireString(s"es.$namespace.host")
    val port = config
      .getIntOption(s"es.$namespace.port")
      .getOrElse(9200)
    val protocol = config
      .getStringOption(s"es.$namespace.protocol")
      .getOrElse("http")

    (
      config.getStringOption(s"es.$namespace.username"),
      config.getStringOption(s"es.$namespace.password"),
      config.getStringOption(s"es.$namespace.apikey")
    ) match {
      case (Some(username), Some(password), None) =>
        ElasticClientBuilder.create(
          hostname,
          port,
          protocol,
          username,
          password
        )
      // Use an API key if specified, even if username/password are also present
      case (_, _, Some(apiKey)) =>
        ElasticClientBuilder.create(hostname, port, protocol, apiKey)
      case _ =>
        throw new Throwable(
          s"You must specify username and password, or apikey, in the 'es.$namespace' config")
    }
  }
}
