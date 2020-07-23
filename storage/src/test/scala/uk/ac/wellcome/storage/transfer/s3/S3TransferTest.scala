package uk.ac.wellcome.storage.transfer.s3

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.s3.S3TypedStore
import uk.ac.wellcome.storage.tags.s3.S3Tags
import uk.ac.wellcome.storage.transfer.{Transfer, TransferNoOp, TransferSourceFailure, TransferTestCases}
import uk.ac.wellcome.storage.{Identified, ObjectLocation}

class S3TransferTest
    extends TransferTestCases[ObjectLocation, ObjectLocation, Record, Bucket, Bucket, S3TypedStore[Record], S3TypedStore[Record], Unit]
    with S3TransferFixtures[Record]
    with RecordGenerators {

  override def withTransfer[R](srcStore: S3TypedStore[Record], dstStore: S3TypedStore[Record])(testWith: TestWith[Transfer[ObjectLocation, ObjectLocation], R]): R =
    testWith(
      new S3Transfer()
    )

  override def createT: Record = createRecord

  override def withContext[R](testWith: TestWith[Unit, R]): R =
    testWith(())

  // This test is intended to spot warnings from the SDK if we don't close
  // the dst inputStream correctly.
  it("errors if the destination exists but the source does not") {
    withLocalS3Bucket { bucket =>
      val src = createObjectLocationWith(bucket)
      val dst = createObjectLocationWith(bucket)

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
      val src = createObjectLocationWith(bucket)
      val dst = createObjectLocationWith(bucket)

      val initialEntries = Map(src -> createRecord)

      val s3Tags = new S3Tags()

      withTransferStore(initialEntries) { implicit store =>
        s3Tags
          .update(S3ObjectLocation(src)) { _ =>
            Right(Map("srcTag" -> "srcValue"))
          } shouldBe a[Right[_, _]]

        withTransfer { transfer =>
          transfer.transfer(src, dst) shouldBe a[Right[_, _]]
        }

        s3Tags.get(S3ObjectLocation(src)).right.value.identifiedT shouldBe Map("srcTag" -> "srcValue")
        s3Tags.get(S3ObjectLocation(dst)).right.value.identifiedT shouldBe empty
      }
    }
  }

  it("allows a no-op copy if the source and destination are the same") {
    withLocalS3Bucket { bucket =>
      val src = createObjectLocationWith(bucket)
      val t = createT

      withTransferStore(initialEntries = Map(src -> t)) { implicit store =>
        val result =
          withTransfer {
            _.transfer(src, src)
          }

        result.right.value shouldBe TransferNoOp(src, src)

        store.get(src) shouldBe Right(Identified(src, t))
      }
    }
  }
}
