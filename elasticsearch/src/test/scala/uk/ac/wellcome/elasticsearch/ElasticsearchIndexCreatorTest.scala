package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.{RequestFailure, Response}
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TestObject(
  id: String,
  description: String,
  visible: Boolean
)

case class CompatibleTestObject(
  id: String,
  description: String,
  visible: Boolean,
  count: Int
)

case class BadTestObject(
  id: String,
  weight: Int
)

class ElasticsearchIndexCreatorTest
    extends AnyFunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with BeforeAndAfterEach {

  val indexFields = Seq(
    keywordField("id"),
    textField("description"),
    booleanField("visible")
  )

  object TestIndexConfig extends IndexConfig {
    val mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )).dynamic(DynamicMapping.Strict)
    val analysis = Analysis(Nil)
    val queries = Nil
  }

  object CompatibleTestIndexConfig extends IndexConfig {
    val mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible"),
        intField("count")
      )).dynamic(DynamicMapping.Strict)
    val analysis = Analysis(Nil)
    val queries = Nil
  }

  it("creates an index into which doc of the expected type can be put") {
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
      val testObject = TestObject("id", "description", visible = true)
      val testObjectJson = toJson(testObject).get

      eventually {
        for {
          _ <- elasticClient.execute(indexInto(index.name).doc(testObjectJson))
          response: Response[SearchResponse] <- elasticClient
            .execute {
              search(index).matchAllQuery()
            }
        } yield {
          val hits = response.result.hits.hits
          hits should have size 1

          assertJsonStringsAreEqual(
            hits.head.sourceAsString,
            testObjectJson
          )
        }
      }
    }
  }

  it("create an index where inserting a doc of an unexpected type fails") {
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = toJson(badTestObject).get

      val future: Future[Response[IndexResponse]] =
        elasticClient
          .execute {
            indexInto(index.name)
              .doc(badTestObjectJson)
          }

      whenReady(future) { response =>
        response.isError shouldBe true
        response shouldBe a[RequestFailure]
      }
    }
  }

  it("updates an already existing index with a compatible mapping") {
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
      withLocalElasticsearchIndex(CompatibleTestIndexConfig, index = index) {
        _ =>
          val compatibleTestObject = CompatibleTestObject(
            id = "id",
            description = "description",
            count = 5,
            visible = true
          )

          val compatibleTestObjectJson = toJson(compatibleTestObject).get

          val futureInsert: Future[Response[IndexResponse]] =
            elasticClient
              .execute {
                indexInto(index.name)
                  .doc(compatibleTestObjectJson)
              }

          whenReady(futureInsert) { response =>
            if (response.isError) { println(response) }
            response.isError shouldBe false

            eventually {
              val response: Response[SearchResponse] =
                elasticClient.execute {
                  search(index).matchAllQuery()
                }.await

              val hits = response.result.hits.hits

              hits should have size 1

              assertJsonStringsAreEqual(
                hits.head.sourceAsString,
                compatibleTestObjectJson
              )
            }
          }
      }
    }
  }
}
