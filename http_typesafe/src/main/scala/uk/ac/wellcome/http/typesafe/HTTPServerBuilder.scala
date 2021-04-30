package uk.ac.wellcome.http.typesafe

import java.net.URL

import com.typesafe.config.Config
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
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

  def buildContextURL(config: Config): URL =
    new URL(config.requireString("contextURL"))
}
