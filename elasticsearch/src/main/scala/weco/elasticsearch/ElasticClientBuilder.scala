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

class ElasticHttpClientConfig(username: String,
                              password: String,
                              apiCompatibleWith: Option[String])
    extends HttpClientConfigCallback {
  val credentials = new UsernamePasswordCredentials(username, password)
  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  // See https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-api-compatibility.html#_rest_api_compatibility_workflow
  private val defaultHeaders = apiCompatibleWith
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
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder
      .setDefaultHeaders(defaultHeaders.asJava)
      .setDefaultCredentialsProvider(credentialsProvider)
      // Enabling TCP keepalive
      // https://github.com/elastic/elasticsearch/issues/65213
      .setDefaultIOReactorConfig(
        IOReactorConfig
          .custom()
          .setSoKeepAlive(true)
          .build()
      )
  }
}

object ElasticClientBuilder {
  private val apiCompatibility = Some("8")

  def create(hostname: String,
             port: Int,
             protocol: String,
             username: String,
             password: String): ElasticClient = {
    val restClient = RestClient
      .builder(new HttpHost(hostname, port, protocol))
      .setHttpClientConfigCallback(
        new ElasticHttpClientConfig(username, password, apiCompatibility))
      .setCompressionEnabled(true)
      .build()

    ElasticClient(JavaClient.fromRestClient(restClient))
  }
}
