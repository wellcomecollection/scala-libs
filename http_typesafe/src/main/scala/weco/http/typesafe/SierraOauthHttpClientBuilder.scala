package weco.http.typesafe

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig._
import weco.http.client.sierra.SierraOauthHttpClient
import weco.http.client.{AkkaHttpClient, HttpGet, HttpPost}

import scala.concurrent.ExecutionContext

object SierraOauthHttpClientBuilder {
  def build(config: Config)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraOauthHttpClient = {
    val username = config.requireString("sierra.api.key")
    val password = config.requireString("sierra.api.secret")

    val client = new AkkaHttpClient() with HttpGet with HttpPost {
      override val baseUri: Uri = Uri(
        config.requireString("sierra.api.baseUrl")
      )
    }

    new SierraOauthHttpClient(
      client,
      credentials = BasicHttpCredentials(
        username = username,
        password = password
      )
    )
  }
}
