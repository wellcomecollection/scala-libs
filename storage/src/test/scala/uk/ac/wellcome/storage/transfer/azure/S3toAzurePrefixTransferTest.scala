package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ListingFailure
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.fixtures.{AzureFixtures, S3Fixtures}
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.listing.s3.{S3ObjectLocationListing, S3ObjectSummaryListing}
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import uk.ac.wellcome.storage.store.azure.{AzureStreamStore, AzureTypedStore}
import uk.ac.wellcome.storage.store.s3.{S3StreamReadable, S3StreamStore, S3TypedStore}
import uk.ac.wellcome.storage.transfer._

class S3toAzurePrefixTransferTest extends PrefixTransferTestCases[
  S3ObjectLocation,
  S3ObjectLocationPrefix,
  AzureBlobLocation,
  AzureBlobLocationPrefix,
  Record,
  Bucket, Container,
  S3TypedStore[Record], AzureTypedStore[Record],
  Unit]
    with RecordGenerators
    with AzureFixtures
    with S3Fixtures {
  type TransferImpl =
    PrefixTransfer[
      S3ObjectLocation,
      S3ObjectLocationPrefix,
      AzureBlobLocation,
      AzureBlobLocationPrefix]

  override def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { srcBucket =>
      testWith(srcBucket)
    }

  override def withDstNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { dstContainer =>
      testWith(dstContainer)
    }

  override def createSrcLocation(srcBucket: Bucket): S3ObjectLocation =
    createObjectLocationWith(srcBucket)

  override def createDstLocation(dstContainer: Container): AzureBlobLocation =
    createBlobLocationWith(dstContainer)

  override def createSrcPrefix(srcBucket: Bucket): S3ObjectLocationPrefix =
    createObjectLocationPrefixWith(srcBucket)

  override def createDstPrefix(dstContainer: Container): AzureBlobLocationPrefix =
    createBlobLocationPrefixWith(dstContainer)

  override def withSrcStore[R](initialEntries: Map[S3ObjectLocation, Record])(testWith: TestWith[S3TypedStore[Record], R])(implicit context: Unit): R = {
    val s3TypedStore = S3TypedStore[Record]

    initialEntries.foreach { case (location, record) =>
      s3TypedStore.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(s3TypedStore)
  }

  override def withDstStore[R](initialEntries: Map[AzureBlobLocation, Record])(testWith: TestWith[AzureTypedStore[Record], R])(implicit context: Unit): R = {
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

  override def withPrefixTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[TransferImpl, R]): R = {
    testWith(new S3toAzurePrefixTransfer())
  }

  override def withExtraListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[TransferImpl, R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val extraListing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: S3ObjectLocationPrefix): ListingResult =
          super.list(prefix).map { _ ++ Seq(createObjectLocation) }
      }

    testWith(new S3toAzurePrefixTransfer()(transfer, extraListing))
  }

  override def withBrokenListingTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[TransferImpl, R]): R = {
    implicit val summaryListing: S3ObjectSummaryListing =
      new S3ObjectSummaryListing()
    implicit val brokenListing: S3ObjectLocationListing =
      new S3ObjectLocationListing() {
        override def list(prefix: S3ObjectLocationPrefix): ListingResult =
          Left(ListingFailure(prefix))
      }

    testWith(new S3toAzurePrefixTransfer()(transfer, brokenListing))
  }

  override def withBrokenTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[TransferImpl, R]): R = {
    implicit val brokenTransfer: S3toAzureTransfer = new S3toAzureTransfer() {
      override def transfer(
        src: S3ObjectLocation,
        dst: AzureBlobLocation,
        checkForExisting: Boolean = true
      ): Either[TransferFailure, TransferSuccess] =
        Left(TransferSourceFailure(src, dst))
    }

    testWith(new S3toAzurePrefixTransfer()(brokenTransfer, s3Listing))
  }

  override def createT: Record = createRecord

  override def withContext[R](testWith: TestWith[Unit, R]): R = testWith(())
}
