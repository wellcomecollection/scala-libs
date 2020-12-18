package uk.ac.wellcome.storage.s3

import software.amazon.awssdk.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import software.amazon.awssdk.client.builder.AwsClientBuilder.EndpointConfiguration
import software.amazon.awssdk.services.s3.{AmazonS3, AmazonS3ClientBuilder}

object S3ClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): AmazonS3 = {
    val standardClient = AmazonS3ClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(region)
        .build()
    else
      standardClient
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)))
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()
  }
}
