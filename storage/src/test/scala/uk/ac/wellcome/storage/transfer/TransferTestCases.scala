package uk.ac.wellcome.storage.transfer

import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.Identified
import uk.ac.wellcome.storage.store.Store

trait TransferTestCases[
  Location, T,
  SrcNamespace, DstNamespace,
  SrcStore <: Store[Location, T],
  DstStore <: Store[Location, T],
  Context]
    extends AnyFunSpec
    with Matchers
    with EitherValues {

  def createT: T

  def withContext[R](testWith: TestWith[Context, R]): R

  def withSrcNamespace[R](testWith: TestWith[SrcNamespace, R]): R
  def withDstNamespace[R](testWith: TestWith[DstNamespace, R]): R

  def withNamespacePair[R](testWith: TestWith[(SrcNamespace, DstNamespace), R]): R =
    withSrcNamespace { srcNamespace =>
      withDstNamespace { dstNamespace =>
        testWith((srcNamespace, dstNamespace))
      }
    }

  def createSrcLocation(namespace: SrcNamespace): Location
  def createDstLocation(namespace: DstNamespace): Location

  def withSrcStore[R](initialEntries: Map[Location, T])(testWith: TestWith[SrcStore, R])(implicit context: Context): R
  def withDstStore[R](initialEntries: Map[Location, T])(testWith: TestWith[DstStore, R])(implicit context: Context): R

  def withTransfer[R](srcStore: SrcStore, dstStore: DstStore)(testWith: TestWith[Transfer[Location], R]): R

  describe("behaves as a Transfer") {
    it("copies an object from a source to a destination") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        val t = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> t)) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst)
                }

              result.right.value shouldBe TransferPerformed(src, dst)

              srcStore.get(src) shouldBe Right(Identified(src, t))
              dstStore.get(dst) shouldBe Right(Identified(dst, t))
            }
          }
        }
      }
    }

    it("errors if the source does not exist") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        withContext { implicit context =>
          withSrcStore(initialEntries = Map.empty) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst)
                }

              result.left.value shouldBe a[TransferSourceFailure[_, _]]

              val failure = result.left.value.asInstanceOf[TransferSourceFailure[Location, Location]]
              failure.src shouldBe src
              failure.dst shouldBe dst
            }
          }
        }
      }
    }

    it("errors if the source and destination both exist and are different") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        val srcT = createT
        val dstT = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> srcT)) { srcStore =>
            withDstStore(initialEntries = Map(dst -> dstT)) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst)
                }

              result.left.value shouldBe a[TransferOverwriteFailure[_, _]]

              val failure = result.left.value.asInstanceOf[TransferOverwriteFailure[Location, Location]]
              failure.src shouldBe src
              failure.dst shouldBe dst

              srcStore.get(src) shouldBe Right(Identified(src, srcT))
              dstStore.get(dst) shouldBe Right(Identified(dst, dstT))
            }
          }
        }
      }
    }

    it(
      "allows a no-op copy if the source and destination both exist and are the same") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        val t = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> t)) { srcStore =>
            withDstStore(initialEntries = Map(dst -> t)) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst)
                }

              result.right.value shouldBe TransferNoOp(src, dst)
              srcStore.get(src) shouldBe Right(Identified(src, t))
              dstStore.get(dst) shouldBe Right(Identified(dst, t))
            }
          }
        }
      }
    }

    it("fails if the source is absent and checkForExisting=false") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        withContext { implicit context =>
          withSrcStore(initialEntries = Map.empty) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst, checkForExisting = false)
                }

              result.left.value shouldBe a[TransferSourceFailure[_, _]]

              val failure = result.left.value.asInstanceOf[TransferSourceFailure[Location, Location]]
              failure.src shouldBe src
              failure.dst shouldBe dst
            }
          }
        }
      }
    }

    it("overwrites the destination if checkForExisting=false") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val src = createSrcLocation(srcNamespace)
        val dst = createDstLocation(dstNamespace)

        val srcT = createT
        val dstT = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> srcT)) { srcStore =>
            withDstStore(initialEntries = Map(dst -> dstT)) { dstStore =>
              val result =
                withTransfer(srcStore, dstStore) {
                  _.transfer(src, dst, checkForExisting = false)
                }

              result.right.value shouldBe TransferPerformed(src, dst)

              srcStore.get(src) shouldBe Right(Identified(src, srcT))
              dstStore.get(dst) shouldBe Right(Identified(dst, srcT))
            }
          }
        }
      }
    }
  }
}
