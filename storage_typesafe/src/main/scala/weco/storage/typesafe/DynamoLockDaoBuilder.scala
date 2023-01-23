package weco.storage.typesafe

import com.typesafe.config.Config
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage.locking.dynamo.{DynamoLockDao, DynamoLockDaoConfig}
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object DynamoLockDaoBuilder {
  def buildDynamoLockDao(config: Config, namespace: String = "locking")(
    implicit
    ec: ExecutionContext,
  ) = new DynamoLockDao(
    client = DynamoDbClient.builder().build(),
    config = DynamoLockDaoConfig(
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config, namespace),
      expiryTime = config
        .getDurationOption(s"aws.$namespace.dynamo.lockExpiryTime")
        .getOrElse(3.minutes)
    )
  )
}
