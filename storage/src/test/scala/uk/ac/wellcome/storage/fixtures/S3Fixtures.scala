package uk.ac.wellcome.storage.fixtures

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.iterable.S3Objects
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, S3ObjectSummary}
import grizzled.slf4j.Logging
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import uk.ac.wellcome.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.generators.ObjectLocationGenerators
import uk.ac.wellcome.storage.s3.{S3ClientFactory, S3Config}
import uk.ac.wellcome.storage.streaming.Codec._
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

import scala.collection.JavaConverters._
import scala.util.Random

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
    with ObjectLocationGenerators {

  import S3Fixtures._

  protected val localS3EndpointUrl = "http://localhost:33333"
  private val regionName = "localhost"

  protected val accessKey = "accessKey1"
  protected val secretKey = "verySecretKey1"

  implicit val s3Client: AmazonS3 = S3ClientFactory.create(
    region = regionName,
    endpoint = localS3EndpointUrl,
    accessKey = accessKey,
    secretKey = secretKey
  )

  val brokenS3Client: AmazonS3 = S3ClientFactory.create(
    region = "nuh-uh",
    endpoint = "http://nope.nope",
    accessKey = randomAlphanumeric,
    secretKey = randomAlphanumeric
  )

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

  def getContentFromS3(location: ObjectLocation): String = {
    val s3Object = s3Client.getObject(location.namespace, location.path)

    val inputStream = new InputStreamWithLength(
      s3Object.getObjectContent,
      length = s3Object.getObjectMetadata.getContentLength
    )
    stringCodec.fromStream(inputStream).right.value
  }

  def getJsonFromS3(location: ObjectLocation): Json =
    parse(getContentFromS3(location)).right.get

  def getObjectFromS3[T](location: ObjectLocation)(
    implicit decoder: Decoder[T]): T =
    fromJson[T](getContentFromS3(location)).get

  def createBucketName: String =
    // Bucket names
    //  - start with a lowercase letter or number,
    //  - do not contain uppercase characters or underscores,
    //  - between 3 and 63 characters in length.
    // [https://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html#bucketnamingrules]
    randomAlphanumeric.toLowerCase

  def createInvalidBucketName: String =
    // Create a variety of invalid patterns, and choose one at random.
    Random
      .shuffle(
        Seq(
          "_" + createBucket,
          randomAlphanumeric.toUpperCase() + createBucket,
          createBucket + randomAlphanumeric.toUpperCase(),
          Random.alphanumeric.take(100) mkString
        ))
      .head

  def createBucket: Bucket =
    Bucket(createBucketName)

  def createObjectLocationWith(
    bucket: Bucket
  ): ObjectLocation =
    ObjectLocation(
      namespace = bucket.name,
      path = randomAlphanumeric
    )

  def createObjectLocationWith(
    bucket: Bucket,
    key: String
  ): ObjectLocation =
    ObjectLocation(
      namespace = bucket.name,
      path = key
    )

  def putStream(location: ObjectLocation, inputStream: InputStreamWithLength): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(inputStream.length)

    val putObjectRequest = new PutObjectRequest(
      location.namespace,
      location.path,
      inputStream,
      metadata
    )

    s3Client.putObject(putObjectRequest)

    inputStream.close()
  }

  def assertEqualObjects(x: ObjectLocation, y: ObjectLocation): Assertion =
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
      key -> getContentFromS3(createObjectLocationWith(bucket, key))
    }.toMap

  def createS3ConfigWith(bucket: Bucket): S3Config =
    S3Config(bucketName = bucket.name)
}