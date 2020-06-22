package uk.ac.wellcome.storage.listing.s3

import java.nio.file.Paths

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
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
    createObjectLocationWith(bucket)

  def extendIdent(location: S3ObjectLocation,
                  extension: String): S3ObjectLocation =
    location.copy(
      key = Paths.get(location.key, extension).normalize().toString
    )

  def createPrefix: S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(
      bucket = createBucketName,
      keyPrefix = randomAlphanumeric
    )

  def createPrefixMatching(location: S3ObjectLocation): S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(
      bucket = location.bucket,
      keyPrefix = location.key
    )

  def withListingContext[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  def createS3Listing(batchSize: Int = 1000)(
    implicit s3Client: AmazonS3 = s3Client): S3Listing[ListingResult]

  def createInitialEntries(
    bucket: Bucket,
    initialEntries: Seq[S3ObjectLocation]): Seq[PutObjectResult] =
    initialEntries
      .map { loc =>
        s3Client.putObject(loc.bucket, loc.key, "hello world")
      }
}
