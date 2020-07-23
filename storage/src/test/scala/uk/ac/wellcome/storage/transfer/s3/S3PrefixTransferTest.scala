package uk.ac.wellcome.storage.transfer.s3

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.listing.s3.{S3ObjectLocationListing, S3ObjectSummaryListing}
import uk.ac.wellcome.storage.store.s3.{S3TypedStore, S3TypedStoreFixtures}
import uk.ac.wellcome.storage.transfer._
import uk.ac.wellcome.storage.{ListingFailure, ObjectLocation, ObjectLocationPrefix}

class S3PrefixTransferTest
    extends PrefixTransferTestCases[
      ObjectLocation,
      ObjectLocationPrefix,
      Record,
      Bucket,
      Bucket,
      S3TypedStore[Record],
      S3TypedStore[Record],
      Unit]
    with RecordGenerators
    with S3TypedStoreFixtures[Record] {

  def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { srcBucket =>
      testWith(srcBucket)
    }

  def withDstNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { dstBucket =>
      testWith(dstBucket)
    }

  def createSrcLocation(srcBucket: Bucket): ObjectLocation =
    createObjectLocationWith(srcBucket)

  def createDstLocation(dstBucket: Bucket): ObjectLocation =
    createObjectLocationWith(dstBucket)

  def createSrcPrefix(srcBucket: Bucket): ObjectLocationPrefix =
    createObjectLocationPrefixWith(srcBucket.name)

  def createDstPrefix(dstBucket: Bucket): ObjectLocationPrefix =
    createObjectLocationPrefixWith(dstBucket.name)

  def createSrcLocationFrom(srcPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    srcPrefix.asLocation(suffix)

  def createDstLocationFrom(dstPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    dstPrefix.asLocation(suffix)

  def withSrcStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[S3TypedStore[Record], R])(implicit context: Unit): R =
    withTypedStoreImpl(context, initialEntries = initialEntries) { typedStore =>
      testWith(typedStore)
    }

  def withDstStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[S3TypedStore[Record], R])(implicit context: Unit): R =
    withTypedStoreImpl(context, initialEntries = initialEntries) { typedStore =>
      testWith(typedStore)
    }

  def withPrefixTransfer[R](srcStore: S3TypedStore[Record], dstStore: S3TypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R =
    testWith(
      S3PrefixTransfer()
    )

  def withExtraListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: S3TypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val listing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: ObjectLocationPrefix): ListingResult =
          super.list(prefix).map { _ ++ Seq(createObjectLocation) }
      }

    implicit val transfer: S3Transfer = new S3Transfer()

    testWith(new S3PrefixTransfer())
  }

  def withBrokenListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: S3TypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val listing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: ObjectLocationPrefix): ListingResult =
          Left(ListingFailure(prefix))
      }

    implicit val transfer: S3Transfer = new S3Transfer()

    testWith(new S3PrefixTransfer())
  }

  def withBrokenTransfer[R](srcStore: S3TypedStore[Record], dstStore: S3TypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val listing: S3ObjectLocationListing = S3ObjectLocationListing()

    implicit val transfer: S3Transfer = new S3Transfer() {
      override def transfer(
                             src: ObjectLocation,
                             dst: ObjectLocation,
                             checkForExisting: Boolean = true): TransferEither =
        Left(TransferSourceFailure(src, dst))
    }

    testWith(new S3PrefixTransfer())
  }

  def createT: Record = createRecord

  def withContext[R](testWith: TestWith[Unit, R]): R = testWith(())
}
