package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.{AzureFixtures, S3Fixtures}
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.listing.s3.{S3ObjectLocationListing, S3ObjectSummaryListing}
import uk.ac.wellcome.storage.store.azure.{AzureStreamStore, AzureTypedStore}
import uk.ac.wellcome.storage.store.s3.{S3StreamReadable, S3StreamStore, S3TypedStore}
import uk.ac.wellcome.storage.{ListingFailure, ObjectLocation, ObjectLocationPrefix}
import uk.ac.wellcome.storage.transfer._

class S3toAzurePrefixTransferTest extends PrefixTransferTestCases[
  ObjectLocation,
  ObjectLocationPrefix,
  Record,
  Bucket, Container,
  S3TypedStore[Record], AzureTypedStore[Record],
  Unit]
    with RecordGenerators
    with AzureFixtures
    with S3Fixtures {
  override def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { srcBucket =>
      testWith(srcBucket)
    }

  override def withDstNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { dstContainer =>
      testWith(dstContainer)
    }

  override def createSrcLocation(srcBucket: Bucket): ObjectLocation =
    createObjectLocationWith(srcBucket)

  override def createDstLocation(dstContainer: Container): ObjectLocation =
    createObjectLocationWith(dstContainer.name)

  override def createSrcPrefix(srcBucket: Bucket): ObjectLocationPrefix =
    createObjectLocationPrefixWith(srcBucket.name)

  override def createDstPrefix(dstContainer: Container): ObjectLocationPrefix =
    createObjectLocationPrefixWith(dstContainer.name)

  override def createSrcLocationFrom(srcPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    srcPrefix.asLocation(suffix)

  override def createDstLocationFrom(dstPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    dstPrefix.asLocation(suffix)

  override def withSrcStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[S3TypedStore[Record], R])(implicit context: Unit): R = {
    val s3TypedStore = S3TypedStore[Record]

    initialEntries.foreach { case (location, record) =>
      s3TypedStore.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(s3TypedStore)
  }

  override def withDstStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[AzureTypedStore[Record], R])(implicit context: Unit): R = {
    implicit val azureStreamStore: AzureStreamStore = new AzureStreamStore()
    val azureTypedStore = new AzureTypedStore[Record]()

    initialEntries.foreach { case (location, record) =>
      azureTypedStore.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(azureTypedStore)
  }

  implicit val s3Readable: S3StreamReadable = new S3StreamStore()
  implicit val s3Listing: S3ObjectLocationListing = S3ObjectLocationListing()
  implicit val transfer: S3toAzureTransfer = new S3toAzureTransfer()

  override def withPrefixTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation], R]): R = {
    testWith(new S3toAzurePrefixTransfer())
  }

  override def withExtraListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val extraListing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: ObjectLocationPrefix): ListingResult =
          super.list(prefix).map { _ ++ Seq(createObjectLocation) }
      }

    testWith(new S3toAzurePrefixTransfer()(transfer, extraListing))
  }

  override def withBrokenListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val brokenListing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: ObjectLocationPrefix): ListingResult =
          Left(ListingFailure(prefix))
      }

    testWith(new S3toAzurePrefixTransfer()(transfer, brokenListing))
  }

  override def withBrokenTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation], R]): R = {
    implicit val brokenTransfer: S3toAzureTransfer = new S3toAzureTransfer() {
      override def transfer(src: ObjectLocation, dst: ObjectLocation, checkForExisting: Boolean = true): TransferEither =
        Left(TransferSourceFailure(src, dst))
    }

    testWith(new S3toAzurePrefixTransfer()(brokenTransfer, s3Listing))
  }

  override def createT: Record = createRecord

  override def withContext[R](testWith: TestWith[Unit, R]): R = testWith(())
}
