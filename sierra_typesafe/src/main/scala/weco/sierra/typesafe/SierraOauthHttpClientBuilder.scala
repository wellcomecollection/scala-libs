package weco.sierra.typesafe

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.headers.BasicHttpCredentials
import com.typesafe.config.Config
import weco.http.client.{HttpGet, HttpPost, PekkoHttpClient}
import weco.sierra.http.SierraOauthHttpClient
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object SierraOauthHttpClientBuilder {
  def build(config: Config)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraOauthHttpClient = {
    val username = config.requireString("sierra.api.key")
    val password = config.requireString("sierra.api.secret")

    val client = new PekkoHttpClient() with HttpGet with HttpPost {
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
