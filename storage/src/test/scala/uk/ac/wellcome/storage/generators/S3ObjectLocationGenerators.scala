package uk.ac.wellcome.storage.generators

import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

import scala.util.Random

trait S3ObjectLocationGenerators extends RandomGenerators {

  def createBucketName: String =
    // Bucket names
    //  - start with a lowercase letter or number,
    //  - do not contain uppercase characters or underscores,
    //  - between 3 and 63 characters in length.
    // [https://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html#bucketnamingrules]
    randomAlphanumeric(length = randomInt(from = 3, to = 63))
      .toLowerCase

  def createBucket: Bucket = Bucket(createBucketName)

  def createInvalidBucketName: String =
    // Create a variety of invalid patterns, and choose one at random.
    Random
      .shuffle(
        Seq(
          "_" + createBucket,
          randomAlphanumeric().toUpperCase() + createBucket,
          createBucket + randomAlphanumeric().toUpperCase(),
          Random.alphanumeric.take(100) mkString
        ))
      .head

  def createInvalidBucket: Bucket = Bucket(createInvalidBucketName)

  def createS3ObjectLocationWith(bucket: Bucket): S3ObjectLocation =
    S3ObjectLocation(
      bucket = bucket.name,
      key = randomAlphanumeric()
    )

  def createS3ObjectLocation: S3ObjectLocation = createS3ObjectLocationWith(bucket = createBucket)

  def createS3ObjectLocationPrefixWith(bucket: Bucket): S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(
      bucket = bucket.name,
      keyPrefix = randomAlphanumeric()
    )

  def createS3ObjectLocationPrefix: S3ObjectLocationPrefix =
    createS3ObjectLocationPrefixWith(bucket = createBucket)

}
