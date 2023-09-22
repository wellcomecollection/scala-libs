package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.http.JavaClient
import org.apache.http.{HttpHeaders, HttpHost}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

import scala.collection.JavaConverters._

class ElasticHttpClientApiKeyConfig(encodedApiKey: String,
                                    apiCompatibleWith: Option[String])
    extends ElasticHttpClientConfig(apiCompatibleWith) {
  override protected def defaultHeaders
    : Seq[BasicHeader] = super.defaultHeaders :+ new BasicHeader(
    "Authorization",
    s"ApiKey $encodedApiKey"
  )
}

class ElasticHttpClientBasicAuthConfig(username: String,
                                       password: String,
                                       apiCompatibleWith: Option[String])
    extends ElasticHttpClientConfig(apiCompatibleWith) {
  private val credentials = new UsernamePasswordCredentials(username, password)
  private val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder =
    super
      .customizeHttpClient(httpClientBuilder)
      .setDefaultCredentialsProvider(credentialsProvider)
}

class ElasticHttpClientConfig(apiCompatibleWith: Option[String])
    extends HttpClientConfigCallback {
  // See https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-api-compatibility.html#_rest_api_compatibility_workflow
  protected def defaultHeaders: Seq[BasicHeader] = apiCompatibleWith
    .map { compatVersion =>
      val compatHeader =
        s"application/vnd.elasticsearch+json;compatible-with=$compatVersion"
      Seq(
        new BasicHeader(HttpHeaders.ACCEPT, compatHeader),
        new BasicHeader(HttpHeaders.CONTENT_TYPE, compatHeader)
      )
    }
    .getOrElse(Nil)
  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder =
    httpClientBuilder
      .setDefaultHeaders(defaultHeaders.asJava)
      // Enabling TCP keepalive
      // https://github.com/elastic/elasticsearch/issues/65213
      .setDefaultIOReactorConfig(
        IOReactorConfig
          .custom()
          .setSoKeepAlive(true)
          .build()
      )
}

object ElasticClientBuilder {
  private val apiCompatibility = Some("8")

  private def fromClientConfig(
    hostname: String,
    port: Int,
    protocol: String,
    clientConfig: ElasticHttpClientConfig): ElasticClient =
    ElasticClient(
      JavaClient.fromRestClient(
        RestClient
          .builder(new HttpHost(hostname, port, protocol))
          .setHttpClientConfigCallback(clientConfig)
          .setCompressionEnabled(true)
          .build()))

  def create(hostname: String,
             port: Int,
             protocol: String,
             encodedApiKey: String): ElasticClient =
    fromClientConfig(
      hostname = hostname,
      port = port,
      protocol = protocol,
      clientConfig =
        new ElasticHttpClientApiKeyConfig(encodedApiKey, apiCompatibility)
    )

  def create(hostname: String,
             port: Int,
             protocol: String,
             username: String,
             password: String): ElasticClient =
    fromClientConfig(
      hostname = hostname,
      port = port,
      protocol = protocol,
      clientConfig = new ElasticHttpClientBasicAuthConfig(
        username = username,
        password = password,
        apiCompatibleWith = apiCompatibility
      )
    )
}
