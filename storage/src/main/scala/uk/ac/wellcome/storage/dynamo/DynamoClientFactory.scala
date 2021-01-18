package uk.ac.wellcome.storage.dynamo

import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

import java.net.URI

object DynamoClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): DynamoDbClient = {
    val standardClient = DynamoDbClient.builder()

    if (endpoint.isEmpty)
      standardClient
        .region(Region.of(region))
        .build()
    else
      standardClient
        .credentialsProvider(
          StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .endpointOverride(new URI(endpoint))
        .build()
  }
}
