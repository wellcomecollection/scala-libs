package weco.fixtures

import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region

import java.net.URI

trait LocalStackFixtures {
  val region: Region = Region.of("localhost")

  val localStackPort = 4566
  val localStackEndpoint = new URI(s"http://localhost:$localStackPort")

  // The LocalStack container divides resources by credentials, e.g. if you connect
  // with key1/secret1, you'll see a different set of resources to key2/secret2.
  //
  // This means it's important for us to use a consistent set of credentials across
  // all uses of localstack, otherwise resources will be not found because they're
  // attached to another "account".
  //
  val credentials: StaticCredentialsProvider =
    StaticCredentialsProvider.create(
      AwsBasicCredentials.create("access", "key"))
}
