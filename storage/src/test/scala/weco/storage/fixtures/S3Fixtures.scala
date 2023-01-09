package weco.storage.fixtures

import grizzled.slf4j.Logging
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Client, S3Configuration}
import software.amazon.awssdk.transfer.s3.S3TransferManager
import weco.fixtures._
import weco.json.JsonUtil._
import weco.storage.generators.{S3ObjectLocationGenerators, StreamGenerators}
import weco.storage.s3.{S3Config, S3ObjectLocation}
import weco.storage.streaming.Codec._
import weco.storage.streaming.InputStreamWithLength

import java.net.URI
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

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

  val s3Credentials =
    StaticCredentialsProvider.create(
      AwsBasicCredentials.create("accessKey1", "verySecretKey1"))

  def createS3ClientWithEndpoint(endpoint: String): S3Client =
    S3Client.builder()
      .credentialsProvider(s3Credentials)
      .forcePathStyle(true)
      .endpointOverride(new URI(endpoint))
      .build()

  def createS3TransferManagerWithEndpoint(endpoint: String): S3TransferManager = {
    val s3AsyncClient =
      S3AsyncClient.builder()
      .credentialsProvider(s3Credentials)
      .forcePathStyle(true)
      .endpointOverride(new URI(endpoint))
      .build()

    S3TransferManager.builder()
      .s3Client(s3AsyncClient)
      .build();
  }

  implicit val s3Client: S3Client =
    createS3ClientWithEndpoint(s"http://localhost:$s3Port")

  implicit val s3TransferManager: S3TransferManager =
    createS3TransferManagerWithEndpoint(s"http://localhost:$s3Port")

  val brokenS3Client: S3Client =
    createS3ClientWithEndpoint("http://nope.nope")

  val brokenS3TransferManager: S3TransferManager =
    createS3TransferManagerWithEndpoint("http://nope.nope")

  implicit val s3Presigner: S3Presigner =
    S3Presigner.builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("accessKey1", "verySecretKey1"))
      )
      .serviceConfiguration(
        S3Configuration.builder()
          .pathStyleAccessEnabled(true)
          .build()
      )
      .endpointOverride(new URI(s"http://localhost:$s3Port"))
      .build()

  private def doesBucketExist(bucketName: String): Boolean = {
    // This is based on a method called doesBucketExistV2 in the V1 Java SDK,
    // which used GetBucketAcl under the hood to check if a bucket existed.
    val request =
      GetBucketAclRequest.builder()
        .bucket(bucketName)
        .build()

    Try { s3Client.getBucketAcl(request) } match {
      case Success(_) => true
      case Failure(e: S3Exception) if e.statusCode() == 404 => false
      case Failure(e) => throw e
    }
  }

  def withLocalS3Bucket[R]: Fixture[Bucket, R] =
    fixture[Bucket, R](
      create = {
        eventually {
          s3Client.listBuckets().buckets().asScala.size should be >= 0
        }
        val bucketName: String = createBucketName

        val request =
          CreateBucketRequest.builder()
            .bucket(bucketName)
            .build()

        s3Client.createBucket(request)

        eventually { doesBucketExist(bucketName) }

        Bucket(bucketName)
      },
      destroy = { bucket: Bucket =>
        if (doesBucketExist(bucket.name)) {

          listKeysInBucket(bucket).foreach { key =>
            safeCleanup(key) { _ =>
              deleteObject(
                S3ObjectLocation(bucket = bucket.name, key = key)
              )
            }
          }

          deleteBucket(bucket)
        } else {
          info(s"Trying to clean up ${bucket.name}, bucket does not exist.")
        }
      }
    )

  def deleteBucket(bucket: Bucket): Unit = {
    val deleteBucketRequest =
      DeleteBucketRequest.builder()
        .bucket(bucket.name)
        .build()

    s3Client.deleteBucket(deleteBucketRequest)
  }

  def deleteObject(location: S3ObjectLocation): Unit = {
    val deleteObjectRequest =
      DeleteObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    s3Client.deleteObject(deleteObjectRequest)
  }

  def getContentFromS3(location: S3ObjectLocation): String = {
    val getRequest =
      GetObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    val s3Object = s3Client.getObject(getRequest)

    val inputStream = new InputStreamWithLength(
      s3Object,
      length = s3Object.response().contentLength()
    )
    stringCodec.fromStream(inputStream).value
  }

  def getJsonFromS3(location: S3ObjectLocation): Json =
    parse(getContentFromS3(location)).right.get

  def getObjectFromS3[T](location: S3ObjectLocation)(
    implicit decoder: Decoder[T]): T =
    fromJson[T](getContentFromS3(location)).get

  def putString(location: S3ObjectLocation, contents: String): Unit = {
    val putRequest =
      PutObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    val requestBody = RequestBody.fromString(contents)

    s3Client.putObject(putRequest, requestBody)
  }

  def putStream(
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength = createInputStream()): Unit = {
    val putRequest =
      PutObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .contentLength(inputStream.length)
        .build()

    val requestBody = RequestBody.fromInputStream(inputStream, inputStream.length)

    s3Client.putObject(putRequest, requestBody)

    inputStream.close()
  }

  def assertEqualObjects(x: S3ObjectLocation, y: S3ObjectLocation): Assertion =
    getContentFromS3(x) shouldBe getContentFromS3(y)

  /** Returns a list of keys in an S3 bucket.
    *
    * @param bucket The instance of S3.Bucket to list.
    * @return A list of object keys.
    */
  def listKeysInBucket(bucket: Bucket): List[String] = {
    val listRequest =
      ListObjectsV2Request.builder()
        .bucket(bucket.name)
        .build()

    s3Client.listObjectsV2Paginator(listRequest)
      .iterator()
      .asScala
      .flatMap { resp: ListObjectsV2Response => resp.contents().asScala }
      .map { s3Obj: S3Object => s3Obj.key() }
      .toList
  }

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
