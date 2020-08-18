package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.Config
import uk.ac.wellcome.storage.locking.dynamo.{
  DynamoLockDao,
  DynamoLockDaoConfig
}
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder

import scala.concurrent.ExecutionContext

object DynamoLockDaoBuilder extends AWSClientConfigBuilder {
  def buildDynamoLockDao(config: Config, namespace: String = "locking")(
    implicit
    ec: ExecutionContext,
  ) = new DynamoLockDao(
    client = DynamoBuilder.buildDynamoClient(config),
    config =
      DynamoLockDaoConfig(DynamoBuilder.buildDynamoConfig(config, namespace))
  )
}
