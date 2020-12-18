package uk.ac.wellcome.storage.dynamo

import software.amazon.awssdk.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import software.amazon.awssdk.client.builder.AwsClientBuilder.EndpointConfiguration
import software.amazon.awssdk.services.dynamodb.{
  DynamoDbClient,
  DynamoDbClientBuilder
}

object DynamoClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): DynamoDbClient = {
    val standardClient = DynamoDbClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(region)
        .build()
    else
      standardClient
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()
  }
}
