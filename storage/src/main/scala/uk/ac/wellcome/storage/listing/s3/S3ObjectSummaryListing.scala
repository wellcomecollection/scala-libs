package uk.ac.wellcome.storage.listing.s3

import software.amazon.awssdk.services.s3.AmazonS3
import software.amazon.awssdk.services.s3.iterable.S3Objects
import software.amazon.awssdk.services.s3.model.S3ObjectSummary
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ListingFailure
import uk.ac.wellcome.storage.s3.S3ObjectLocationPrefix

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class S3ObjectSummaryListing(batchSize: Int = 1000)(
  implicit s3Client: AmazonS3
) extends S3Listing[S3ObjectSummary]
    with Logging {
  override def list(prefix: S3ObjectLocationPrefix): ListingResult = {
    if (!prefix.keyPrefix.endsWith("/") && prefix.keyPrefix != "") {
      warn(
        "Listing an S3 prefix that does not end with a slash " +
          s"($prefix) may return unexpected objects. " +
          "See https://alexwlchan.net/2020/08/s3-prefixes-are-not-directories/"
      )
    }

    Try {
      val iterator = S3Objects
        .withPrefix(
          s3Client,
          prefix.bucket,
          prefix.keyPrefix
        )
        .withBatchSize(batchSize)
        .asScala

      // Because the iterator is lazy, it won't make the initial call to S3 until
      // the caller starts to consume the results.  This can cause an exception to
      // be thrown in user code if, for example, the bucket doesn't exist.
      //
      // Although we discard the result of this toString method immediately, it
      // causes an exception to be thrown here and a Left returned, rather than
      // bubbling up the exception in user code.
      //
      // See the test cases in S3ListingTestCases.
      iterator.toString()

      iterator
    } match {
      case Failure(err)     => Left(ListingFailure(prefix, err))
      case Success(objects) => Right(objects)
    }
  }
}
