package uk.ac.wellcome.storage.typesafe

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import com.typesafe.config.Config
import uk.ac.wellcome.config.models.AWSClientConfig
import uk.ac.wellcome.storage.dynamo.{DynamoClientFactory, DynamoConfig}
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object DynamoBuilder extends AWSClientConfigBuilder {
  def buildDynamoConfig(config: Config,
                        namespace: String = ""): DynamoConfig = {
    val tableName = config
      .requireString(s"aws.$namespace.dynamo.tableName")
    val tableIndex = config
      .getStringOption(s"aws.$namespace.dynamo.tableIndex")
      .getOrElse("")

    DynamoConfig(
      tableName = tableName,
      maybeIndexName = if (tableIndex.isEmpty) None else Some(tableIndex)
    )
  }

  private def buildDynamoClient(
    awsClientConfig: AWSClientConfig): DynamoDbClient =
    DynamoClientFactory.create(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildDynamoClient(config: Config): DynamoDbClient =
    buildDynamoClient(
      awsClientConfig = buildAWSClientConfig(config, namespace = "dynamo")
    )
}
