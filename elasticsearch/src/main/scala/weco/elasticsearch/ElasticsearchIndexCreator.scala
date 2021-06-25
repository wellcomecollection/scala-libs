package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticApi.createIndex
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.{ElasticClient, Index, Response}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchIndexCreator(
  elasticClient: ElasticClient,
  index: Index,
  config: IndexConfig)(implicit ec: ExecutionContext)
    extends Logging {

  def create: Future[Unit] = createOrUpdate

  val mapping = config.mapping
  val analysis = config.analysis

  private def exists =
    elasticClient.execute(indexExists(index.name)).map(_.result.isExists)

  private def createOrUpdate: Future[Unit] = {
    for {
      doesExist <- exists
      createResp <- if (doesExist) update else put
    } yield { handleEsError(createResp) }
  }

  private def put =
    elasticClient
      .execute {
        createIndex(index.name)
          .mapping(mapping)
          .shards(config.shards)
          .analysis(analysis)
          // Elasticsearch has a default maximum number of fields of 1000.
          // Because images have all of the WorkData fields defined twice in the mapping,
          // they end up having more than 1000 fields, so we increase them to 2000
          .settings(Map("mapping.total_fields.limit" -> 2000))
          .refreshInterval(config.refreshInterval.toEsValue)
      }

  private def update = {
    // As this doesn't need to run synchronously, we kick it off here
    val settingsUpdateResp = settingsUpdate
    for {
      originalMapping <- elasticClient.execute(getMapping(index.name))
      originalMeta = originalMapping.result.head.meta
      mergedMeta = originalMeta ++ mapping.meta
      _ <- settingsUpdateResp
      resp <- elasticClient
        .execute(
          putMapping(index.name)
            .dynamic(mapping.dynamic.getOrElse(DynamicMapping.Strict))
            .meta(mergedMeta)
            .as(mapping.properties)
        )
    } yield resp
  }

  private def settingsUpdate = {
    for {
      resp <- elasticClient
        .execute(
          updateSettings(
            index.name,
            Map("index.refresh_interval" -> config.refreshInterval.toEsValue))
        )
    } yield { handleEsError(resp) }
  }

  private def handleEsError[T](resp: Response[T]) =
    if (resp.isError) {
      throw new RuntimeException(
        s"Index creation error on index:${index.name} resp: $resp")
    }
}
