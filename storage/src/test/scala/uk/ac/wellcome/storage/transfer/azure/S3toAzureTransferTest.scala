package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.fixtures.{AzureFixtures, S3Fixtures}
import uk.ac.wellcome.storage.generators.{ObjectLocationGenerators, Record, RecordGenerators}
import uk.ac.wellcome.storage.store.azure.{AzureStreamStore, AzureTypedStore}
import uk.ac.wellcome.storage.store.s3.{S3StreamReadable, S3StreamStore, S3TypedStore}
import uk.ac.wellcome.storage.transfer.{Transfer, TransferTestCases}

class S3toAzureTransferTest extends TransferTestCases[ObjectLocation, Record, Bucket, Container, S3TypedStore[Record], AzureTypedStore[Record], Unit] with RecordGenerators with AzureFixtures with S3Fixtures with ObjectLocationGenerators {
  override def createT: Record = createRecord

  override def withContext[R](testWith: TestWith[Unit, R]): R = testWith(())

  override def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  override def withDstNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  override def createSrcLocation(bucket: Bucket): ObjectLocation =
    createObjectLocationWith(bucket)

  override def createDstLocation(container: Container): ObjectLocation =
    createObjectLocationWith(container.name)

  override def withSrcStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[S3TypedStore[Record], R])(implicit context: Unit): R = {
    implicit val s3StreamStore: S3StreamStore = new S3StreamStore()

    val s3TypedStore = new S3TypedStore[Record]()

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

  override def withTransfer[R](srcStore: S3TypedStore[Record], dstStore: AzureTypedStore[Record])(testWith: TestWith[Transfer[ObjectLocation], R]): R = {
    implicit val s3StreamReadable: S3StreamReadable = srcStore.streamStore

    testWith(new S3toAzureTransfer())
  }
}
