package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.http.JavaClient
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

class ElasticCredentials(username: String, password: String)
    extends HttpClientConfigCallback {
  val credentials = new UsernamePasswordCredentials(username, password)
  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder
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
  def create(hostname: String,
             port: Int,
             protocol: String,
             username: String,
             password: String): ElasticClient = {
    val restClient = RestClient
      .builder(new HttpHost(hostname, port, protocol))
      .setHttpClientConfigCallback(new ElasticCredentials(username, password))
      .setCompressionEnabled(true)
      .build()

    ElasticClient(JavaClient.fromRestClient(restClient))
  }
}
