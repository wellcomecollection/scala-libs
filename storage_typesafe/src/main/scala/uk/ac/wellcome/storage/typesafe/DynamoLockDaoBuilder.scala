package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.Config
import uk.ac.wellcome.storage.locking.dynamo.{
  DynamoLockDao,
  DynamoLockDaoConfig
}
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

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
