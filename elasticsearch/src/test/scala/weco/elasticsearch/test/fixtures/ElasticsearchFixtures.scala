package weco.elasticsearch.test.fixtures

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.cluster.ClusterHealthResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, Index, Response}
import io.circe.parser.parse
import io.circe.{Encoder, Json}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Assertion, Suite}
import weco.elasticsearch._
import weco.elasticsearch.model.IndexId
import weco.fixtures._
import weco.json.JsonUtil.toJson

import scala.concurrent.ExecutionContext.Implicits.global

trait ElasticsearchFixtures
    extends Eventually
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with RandomGenerators { this: Suite =>

  private val esHost = "localhost"
  private val esPort = 9200

  lazy implicit val elasticClient: ElasticClient = ElasticClientBuilder.create(
    hostname = esHost,
    port = esPort,
    protocol = "http",
    username = "elastic",
    password = "changeme",
    compressionEnabled = false,
  )

  lazy val elasticClientWithCompression = ElasticClientBuilder.create(
    hostname = esHost,
    port = esPort,
    protocol = "http",
    username = "elastic",
    password = "changeme",
    compressionEnabled = true,
  )

  // Elasticsearch takes a while to start up so check that it actually started
  // before running tests.
  eventually(Timeout(Span(40, Seconds))) {
    val response: Response[ClusterHealthResponse] = elasticClient
      .execute(clusterHealth())
      .await

    response.result.numberOfNodes shouldBe 1
  }

  def withLocalElasticsearchIndex[R](
    config: IndexConfig,
    index: Index = createIndex): Fixture[Index, R] = fixture[Index, R](
    create = {
      new ElasticsearchIndexCreator(
        elasticClient = elasticClient,
        index = index,
        config = config).create.await

      // Elasticsearch is eventually consistent, so the future
      // completing doesn't actually mean that the index exists yet
      eventuallyIndexExists(index)

      index
    },
    destroy = eventuallyDeleteIndex
  )

  def eventuallyIndexExists(index: Index): Assertion =
    eventually {
      val response: Response[IndexExistsResponse] =
        elasticClient
          .execute(indexExists(index.name))
          .await

      response.result.isExists shouldBe true
    }

  private def eventuallyDeleteIndex(index: Index): Assertion = {
    elasticClient.execute(deleteIndex(index.name))

    eventually {
      val response: Response[IndexExistsResponse] =
        elasticClient
          .execute(indexExists(index.name))
          .await

      response.result.isExists shouldBe false
    }
  }

  def assertElasticsearchEventuallyHas[T](index: Index, documents: T*)(
    implicit id: IndexId[T],
    encoder: Encoder[T]): Seq[Assertion] =
    documents.map { document =>
      val documentJson = toJson(document).get

      eventually {
        val response: Response[GetResponse] = elasticClient.execute {
          get(index, id.indexId(document))
        }.await

        val getResponse = response.result

        getResponse.exists shouldBe true

        assertJsonStringsAreEqualIgnoringNulls(
          getResponse.sourceAsString,
          documentJson)
      }
    }

  def assertElasticsearchEmpty[T](index: Index): Assertion =
    // Elasticsearch is eventually consistent so, when the future completes,
    // the documents won't appear in the search until after a refresh
    eventually {
      val response: Response[SearchResponse] = elasticClient.execute {
        search(index).matchAllQuery()
      }.await

      val hits = response.result.hits.hits

      hits should have size 0
    }

  def assertElasticsearchNeverHas[T](index: Index, documents: T*)(
    implicit id: IndexId[T]): Unit = {
    // Let enough time pass to account for elasticsearch
    // eventual consistency before asserting
    Thread.sleep(500)

    documents.foreach { document =>
      val response: Response[GetResponse] = elasticClient
        .execute(get(index, id.indexId(document)))
        .await

      response.result.found shouldBe false
    }
  }

  def createIndex: Index =
    Index(name = createIndexName)

  def createIndexName: String =
    s"index-${randomAlphanumeric().toLowerCase}"

  def assertJsonStringsAreEqualIgnoringNulls(a: String,
                                             b: String): Assertion = {
    val jsonA = parseOrElse(a)
    val jsonB = parseOrElse(b)
    jsonA shouldBe jsonB
  }

  private def parseOrElse(jsonString: String): Json =
    parse(jsonString) match {
      case Right(json) => json.deepDropNullValues
      case Left(err) => {
        println(s"Error trying to parse string <<$jsonString>>")
        throw err
      }
    }
}
