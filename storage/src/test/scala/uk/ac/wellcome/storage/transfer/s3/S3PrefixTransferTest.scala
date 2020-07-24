package uk.ac.wellcome.storage.transfer.s3

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.listing.s3.{S3ObjectLocationListing, S3ObjectSummaryListing}
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import uk.ac.wellcome.storage.store.s3.{S3TypedStore, S3TypedStoreFixtures}
import uk.ac.wellcome.storage.transfer._
import uk.ac.wellcome.storage.{ListingFailure, ObjectLocation}

class S3PrefixTransferTest
    extends PrefixTransferTestCases[
      S3ObjectLocation, S3ObjectLocationPrefix,
      S3ObjectLocation, S3ObjectLocationPrefix,
      ObjectLocation, ObjectLocation,
      Record,
      Bucket,
      Bucket,
      S3TypedStore[Record],
      S3TypedStore[Record],
      Unit]
    with RecordGenerators
    with S3TypedStoreFixtures[Record]
    with S3TransferFixtures[Record] {

  def createSrcPrefix(srcBucket: Bucket): S3ObjectLocationPrefix =
    createS3ObjectLocationPrefixWith(srcBucket)

  def createDstPrefix(dstBucket: Bucket): S3ObjectLocationPrefix =
    createS3ObjectLocationPrefixWith(dstBucket)

  def createSrcLocationFrom(srcPrefix: S3ObjectLocationPrefix, suffix: String): S3ObjectLocation =
    srcPrefix.asLocation(suffix)

  def createDstLocationFrom(dstPrefix: S3ObjectLocationPrefix, suffix: String): S3ObjectLocation =
    dstPrefix.asLocation(suffix)

  def withPrefixTransfer[R](
    srcStore: S3TypedStore[Record],
    dstStore: S3TypedStore[Record])(
    testWith: TestWith[PrefixTransferImpl, R]
  ): R =
    testWith(S3PrefixTransfer())

  def withExtraListingTransfer[R](
    srcStore: S3TypedStore[Record],
    dstStore: S3TypedStore[Record]
  )(
    testWith: TestWith[PrefixTransferImpl, R]
  ): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val listing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: S3ObjectLocationPrefix): ListingResult =
          super.list(prefix).map { _ ++ Seq(createS3ObjectLocation) }
      }

    implicit val transfer: S3Transfer = new S3Transfer()

    testWith(new S3PrefixTransfer())
  }

  def withBrokenListingTransfer[R](
    srcStore: S3TypedStore[Record],
    dstStore: S3TypedStore[Record]
  )(
    testWith: TestWith[PrefixTransferImpl, R]
  ): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val listing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: S3ObjectLocationPrefix): ListingResult =
          Left(ListingFailure(prefix))
      }

    implicit val transfer: S3Transfer = new S3Transfer()

    testWith(new S3PrefixTransfer())
  }

  def withBrokenTransfer[R](
    srcStore: S3TypedStore[Record],
    dstStore: S3TypedStore[Record]
  )(
    testWith: TestWith[PrefixTransferImpl, R]
  ): R = {
    implicit val listing: S3ObjectLocationListing = S3ObjectLocationListing()

    implicit val transfer: S3Transfer = new S3Transfer() {
      override def transfer(
        src: S3ObjectLocation,
        dst: S3ObjectLocation,
        checkForExisting: Boolean = true
      ): TransferEither =
        Left(TransferSourceFailure(src, dst))
    }

    testWith(new S3PrefixTransfer())
  }

  def createT: Record = createRecord

  def withContext[R](testWith: TestWith[Unit, R]): R = testWith(())

  override def srcToObjectLocation(srcLocation: S3ObjectLocation): ObjectLocation = srcLocation.toObjectLocation

  override def dstToObjectLocation(dstLocation: S3ObjectLocation): ObjectLocation = dstLocation.toObjectLocation
}
