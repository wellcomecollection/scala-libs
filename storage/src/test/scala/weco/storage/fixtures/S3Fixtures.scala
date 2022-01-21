package weco.storage.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.iterable.S3Objects
import com.amazonaws.services.s3.model.{
  ObjectMetadata,
  PutObjectRequest,
  S3ObjectSummary
}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import grizzled.slf4j.Logging
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.fixtures._
import weco.json.JsonUtil._
import weco.storage.generators.{S3ObjectLocationGenerators, StreamGenerators}
import weco.storage.s3.{S3Config, S3ObjectLocation}
import weco.storage.streaming.Codec._
import weco.storage.streaming.InputStreamWithLength

import scala.collection.JavaConverters._

object S3Fixtures {
  class Bucket(val name: String) extends AnyVal {
    override def toString = s"S3.Bucket($name)"
  }

  object Bucket {
    def apply(name: String): Bucket = new Bucket(name)
  }
}

trait S3Fixtures
    extends Logging
    with Eventually
    with IntegrationPatience
    with Matchers
    with EitherValues
    with S3ObjectLocationGenerators
    with StreamGenerators {

  import S3Fixtures._

  val s3Port = 33333

  def createS3ClientWithEndpoint(endpoint: String): AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials("accessKey1", "verySecretKey1")))
      .withPathStyleAccessEnabled(true)
      .withEndpointConfiguration(
        new EndpointConfiguration(endpoint, "localhost"))
      .build()

  implicit val s3Client: AmazonS3 =
    createS3ClientWithEndpoint(s"http://localhost:$s3Port")

  val brokenS3Client: AmazonS3 =
    createS3ClientWithEndpoint("http://nope.nope")

  def withLocalS3Bucket[R]: Fixture[Bucket, R] =
    fixture[Bucket, R](
      create = {
        eventually {
          s3Client.listBuckets().asScala.size should be >= 0
        }
        val bucketName: String = createBucketName
        s3Client.createBucket(bucketName)
        eventually { s3Client.doesBucketExistV2(bucketName) }

        Bucket(bucketName)
      },
      destroy = { bucket: Bucket =>
        if (s3Client.doesBucketExistV2(bucket.name)) {

          listKeysInBucket(bucket).foreach { key =>
            safeCleanup(key) {
              s3Client.deleteObject(bucket.name, _)
            }
          }

          s3Client.deleteBucket(bucket.name)
        } else {
          info(s"Trying to clean up ${bucket.name}, bucket does not exist.")
        }
      }
    )

  def getContentFromS3(location: S3ObjectLocation): String = {
    val s3Object = s3Client.getObject(location.bucket, location.key)

    val inputStream = new InputStreamWithLength(
      s3Object.getObjectContent,
      length = s3Object.getObjectMetadata.getContentLength
    )
    stringCodec.fromStream(inputStream).value
  }

  def getJsonFromS3(location: S3ObjectLocation): Json =
    parse(getContentFromS3(location)).right.get

  def getObjectFromS3[T](location: S3ObjectLocation)(
    implicit decoder: Decoder[T]): T =
    fromJson[T](getContentFromS3(location)).get

  def putStream(
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength = createInputStream()): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(inputStream.length)

    val putObjectRequest = new PutObjectRequest(
      location.bucket,
      location.key,
      inputStream,
      metadata
    )

    s3Client.putObject(putObjectRequest)

    inputStream.close()
  }

  def assertEqualObjects(x: S3ObjectLocation, y: S3ObjectLocation): Assertion =
    getContentFromS3(x) shouldBe getContentFromS3(y)

  /** Returns a list of keys in an S3 bucket.
    *
    * @param bucket The instance of S3.Bucket to list.
    * @return A list of object keys.
    */
  def listKeysInBucket(bucket: Bucket): List[String] =
    S3Objects
      .inBucket(s3Client, bucket.name)
      .withBatchSize(1000)
      .iterator()
      .asScala
      .toList
      .par
      .map { objectSummary: S3ObjectSummary =>
        objectSummary.getKey
      }
      .toList

  /** Returns a map (key -> contents) for all objects in an S3 bucket.
    *
    * @param bucket The instance of S3.Bucket to read.
    *
    */
  def getAllObjectContents(bucket: Bucket): Map[String, String] =
    listKeysInBucket(bucket).map { key =>
      key -> getContentFromS3(S3ObjectLocation(bucket = bucket.name, key = key))
    }.toMap

  def createS3ConfigWith(bucket: Bucket): S3Config =
    S3Config(bucketName = bucket.name)
}
