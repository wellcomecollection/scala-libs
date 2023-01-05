package weco.storage.listing.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.scalatest.Assertion
import weco.storage.s3.S3ObjectLocation

class S3ObjectListingTest extends S3ListingTestCases[S3ObjectSummary] {
  override def assertResultCorrect(
    result: Iterable[S3ObjectSummary],
    entries: Seq[S3ObjectLocation]): Assertion = {
    val actualLocations =
      result.toSeq.map { summary =>
        S3ObjectLocation(summary)
      }

    actualLocations should contain theSameElementsAs entries
  }

  override def createS3Listing(implicit s3Client: AmazonS3 = s3Client): S3Listing[S3ObjectSummary] =
    new S3ObjectListing()
}
