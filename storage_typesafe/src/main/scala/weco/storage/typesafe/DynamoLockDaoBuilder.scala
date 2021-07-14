package weco.storage.typesafe

import com.typesafe.config.Config
import weco.storage.locking.dynamo.{DynamoLockDao, DynamoLockDaoConfig}
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object DynamoLockDaoBuilder extends AWSClientConfigBuilder {
  def buildDynamoLockDao(config: Config, namespace: String = "locking")(
    implicit
    ec: ExecutionContext,
  ) = new DynamoLockDao(
    client = DynamoBuilder.buildDynamoClient(config),
    config = DynamoLockDaoConfig(
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config, namespace),
      expiryTime = config
        .getDurationOption(s"aws.$namespace.dynamo.lockExpiryTime")
        .getOrElse(3.minutes)
    )
  )
}
