package uk.ac.wellcome.storage.transfer.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import org.scalatestplus.mockito.MockitoSugar
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.s3.S3TypedStore
import uk.ac.wellcome.storage.tags.s3.S3Tags
import uk.ac.wellcome.storage.transfer.{Transfer, TransferNoOp, TransferPerformed, TransferSourceFailure, TransferTestCases}

class S3TransferTest
    extends TransferTestCases[
      S3ObjectLocation, S3ObjectLocation,
      Record, Bucket,
      Bucket,
      S3TypedStore[Record], S3TypedStore[Record],
      Unit
      ]
    with S3TransferFixtures[Record]
    with RecordGenerators with MockitoSugar{

  override def withTransfer[R](
    srcStore: S3TypedStore[Record],
    dstStore: S3TypedStore[Record])(testWith: TestWith[Transfer[S3ObjectLocation, S3ObjectLocation], R]
  ): R =
    testWith(new S3Transfer())

  override def createT: Record = createRecord

  override def withContext[R](testWith: TestWith[Unit, R]): R =
    testWith(())

  // This test is intended to spot warnings from the SDK if we don't close
  // the dst inputStream correctly.
  it("errors if the destination exists but the source does not") {
    withLocalS3Bucket { bucket =>
      val src = createS3ObjectLocationWith(bucket)
      val dst = createS3ObjectLocationWith(bucket)

      val initialEntries = Map(dst -> createRecord)

      withTransferStore(initialEntries) { implicit store =>
        withTransfer { transfer =>
          transfer
            .transfer(src, dst)
            .left
            .value shouldBe a[TransferSourceFailure[_, _]]
        }
      }
    }
  }

  it("doesn't replicate tags") {
    withLocalS3Bucket { bucket =>
      val src = createS3ObjectLocationWith(bucket)
      val dst = createS3ObjectLocationWith(bucket)

      val initialEntries = Map(src -> createRecord)

      val s3Tags = new S3Tags()

      withTransferStore(initialEntries) { implicit store =>
        s3Tags
          .update(src) { _ =>
            Right(Map("srcTag" -> "srcValue"))
          } shouldBe a[Right[_, _]]

        withTransfer { transfer =>
          transfer.transfer(src, dst) shouldBe a[Right[_, _]]
        }

        s3Tags.get(src).right.value.identifiedT shouldBe Map("srcTag" -> "srcValue")
        s3Tags.get(dst).right.value.identifiedT shouldBe empty
      }
    }
  }

  it("allows a no-op copy if the source and destination are the same") {
    withLocalS3Bucket { bucket =>
      val src = createS3ObjectLocationWith(bucket)
      val t = createT

      withTransferStore(initialEntries = Map(src -> t)) { implicit store =>
        val result =
          withTransfer {
            _.transfer(src, src)
          }

        result.right.value shouldBe TransferNoOp(src, src)

        store.get(src).right.value.identifiedT shouldBe t
      }
    }
  }

  it("retries 500 errors from S3"){
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        val t = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> t)) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
            val failingOnceTransfer = mock[TransferManager]
            val transfer = new S3Transfer()

              val result = transfer.transfer(src, dst)


              result.right.value shouldBe TransferPerformed(src, dst)

              srcStore.get(src).right.value.identifiedT shouldBe t
              dstStore.get(dst).right.value.identifiedT shouldBe t
            }
          }
        }
      }
    }
}
