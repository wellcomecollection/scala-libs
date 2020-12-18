package uk.ac.wellcome.storage.dynamo

import java.net.URI
import software.amazon.awssdk.auth.credentials.{StaticCredentialsProvider, AwsBasicCredentials}
import software.amazon.awssdk.services.dynamodb.{
  DynamoDbClient,
  DynamoDbClientBuilder
}
import software.amazon.awssdk.regions.Region

object DynamoClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): DynamoDbClient = {
    val builder = DynamoDbClient.builder().region(Region.of(region))
    if (endpoint.isEmpty)
      builder.build()
    else
      builder
        .credentialsProvider(
          StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .endpointOverride(URI.create(endpoint))
        .build()
  }
}
