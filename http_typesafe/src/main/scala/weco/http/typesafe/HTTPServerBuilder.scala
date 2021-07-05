package weco.http.typesafe

import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig._
import weco.http.models.HTTPServerConfig

object HTTPServerBuilder {
  def buildHTTPServerConfig(config: Config): HTTPServerConfig = {
    val host = config.requireString("http.host")
    val port = config.requireInt("http.port")
    val externalBaseURL = config.requireString("http.externalBaseURL")

    HTTPServerConfig(
      host = host,
      port = port,
      externalBaseURL = externalBaseURL
    )
  }
}
