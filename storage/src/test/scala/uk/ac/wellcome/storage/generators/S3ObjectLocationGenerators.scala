package uk.ac.wellcome.storage.generators

import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

trait S3ObjectLocationGenerators extends RandomThings {
  def createBucketName: String =
    // Bucket names
    //  - start with a lowercase letter or number,
    //  - do not contain uppercase characters or underscores,
    //  - between 3 and 63 characters in length.
    // [https://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html#bucketnamingrules]
    randomAlphanumeric.toLowerCase

  def createBucket: Bucket = Bucket(createBucketName)

  def createS3ObjectLocationWith(bucket: Bucket): S3ObjectLocation =
    S3ObjectLocation(
      bucket = bucket.name,
      key = randomAlphanumeric
    )

  def createS3ObjectLocation: S3ObjectLocation = createS3ObjectLocationWith(bucket = createBucket)

  def createS3ObjectLocationPrefixWith(bucket: Bucket): S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(
      bucket = bucket.name,
      keyPrefix = randomAlphanumeric
    )

  def createS3ObjectLocationPrefix: S3ObjectLocationPrefix =
    createS3ObjectLocationPrefixWith(bucket = createBucket)

}
