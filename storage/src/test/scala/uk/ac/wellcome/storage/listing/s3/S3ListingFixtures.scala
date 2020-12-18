package uk.ac.wellcome.storage.listing.s3

import software.amazon.awssdk.services.s3.AmazonS3
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.listing.fixtures.ListingFixtures
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

trait S3ListingFixtures[ListingResult]
    extends S3Fixtures
    with ListingFixtures[
      S3ObjectLocation,
      S3ObjectLocationPrefix,
      ListingResult,
      S3Listing[ListingResult],
      Bucket] {
  def createIdent(implicit bucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(bucket)

  def extendIdent(location: S3ObjectLocation,
                  extension: String): S3ObjectLocation =
    location.join(extension)

  def createPrefix: S3ObjectLocationPrefix =
    createS3ObjectLocationPrefixWith(createBucket)

  def createPrefixMatching(location: S3ObjectLocation): S3ObjectLocationPrefix =
    location.asPrefix

  def withListingContext[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  def createS3Listing(batchSize: Int = 1000)(
    implicit s3Client: AmazonS3 = s3Client): S3Listing[ListingResult]
}
