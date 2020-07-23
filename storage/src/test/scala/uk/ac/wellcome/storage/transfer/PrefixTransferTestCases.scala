package uk.ac.wellcome.storage.transfer

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.Identified
import uk.ac.wellcome.storage.store.Store

trait PrefixTransferTestCases[
  Location, Prefix, T,
  SrcNamespace, DstNamespace,
  SrcStore <: Store[Location, T],
  DstStore <: Store[Location, T],
  Context]
    extends AnyFunSpec
    with Matchers
    with EitherValues {
  def withSrcNamespace[R](testWith: TestWith[SrcNamespace, R]): R
  def withDstNamespace[R](testWith: TestWith[DstNamespace, R]): R

  def withNamespacePair[R](testWith: TestWith[(SrcNamespace, DstNamespace), R]): R =
    withSrcNamespace { srcNamespace =>
      withDstNamespace { dstNamespace =>
        testWith((srcNamespace, dstNamespace))
      }
    }

  def createSrcLocation(srcNamespace: SrcNamespace): Location
  def createDstLocation(dstNamespace: DstNamespace): Location

  def createSrcPrefix(srcNamespace: SrcNamespace): Prefix
  def createDstPrefix(dstNamespace: DstNamespace): Prefix

  def createSrcLocationFrom(srcPrefix: Prefix, suffix: String): Location
  def createDstLocationFrom(dstPrefix: Prefix, suffix: String): Location

  def withSrcStore[R](initialEntries: Map[Location, T])(testWith: TestWith[SrcStore, R])(implicit context: Context): R
  def withDstStore[R](initialEntries: Map[Location, T])(testWith: TestWith[DstStore, R])(implicit context: Context): R

  def withPrefixTransfer[R](srcStore: SrcStore, dstStore: DstStore)(testWith: TestWith[PrefixTransfer[Prefix, Location], R]): R

  def withExtraListingTransfer[R](srcStore: SrcStore, dstStore: DstStore)(testWith: TestWith[PrefixTransfer[Prefix, Location], R]): R
  def withBrokenListingTransfer[R](srcStore: SrcStore, dstStore: DstStore)(testWith: TestWith[PrefixTransfer[Prefix, Location], R]): R
  def withBrokenTransfer[R](srcStore: SrcStore, dstStore: DstStore)(testWith: TestWith[PrefixTransfer[Prefix, Location], R]): R

  def createT: T

  def withContext[R](testWith: TestWith[Context, R]): R

  describe("behaves as a PrefixTransfer") {
    it("does nothing if the prefix is empty") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        withContext { implicit context =>
          withSrcStore(initialEntries = Map.empty) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(
                    srcPrefix = srcPrefix,
                    dstPrefix = dstPrefix
                  )
                }

              result.right.value shouldBe PrefixTransferSuccess(0)
            }
          }
        }
      }
    }

    it("copies a single object") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocation = createSrcLocationFrom(srcPrefix, suffix = "1.txt")
        val dstLocation = createDstLocationFrom(dstPrefix, suffix = "1.txt")

        val t = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(srcLocation -> t)) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(
                    srcPrefix = srcPrefix,
                    dstPrefix = dstPrefix
                  )
                }

              result.right.value shouldBe PrefixTransferSuccess(1)

              srcStore.get(srcLocation) shouldBe Right(Identified(srcLocation, t))
              dstStore.get(dstLocation) shouldBe Right(Identified(dstLocation, t))
            }
          }
        }
      }
    }

    it("copies multiple objects") {
      val objectCount = 5

      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocations = (1 to objectCount).map { i =>
          createSrcLocationFrom(srcPrefix, suffix = s"$i.txt")
        }

        val valuesT: Seq[T] = (1 to objectCount).map { _ =>
          createT
        }

        withContext { implicit context =>
          withSrcStore(initialEntries = srcLocations.zip(valuesT).toMap) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              result.right.value shouldBe PrefixTransferSuccess(objectCount)

              // TODO: Check the objects were copied correctly
            }
          }
        }
      }
    }

    it("does not copy items from outside the prefix") {
      val objectCount = 5

      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocations = (1 to objectCount).map { i =>
          createSrcLocationFrom(srcPrefix, suffix = s"$i.txt")
        }

        val valuesT: Seq[T] = (1 to objectCount).map { _ => createT }

        val otherPrefix = createSrcPrefix(srcNamespace)
        val otherLocation = createSrcLocationFrom(otherPrefix, suffix = "other.txt")

        val initialEntries = srcLocations.zip(valuesT).toMap ++ Map(
          otherLocation -> createT)

        withContext { implicit context =>
          withSrcStore(initialEntries = initialEntries) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              result.right.value shouldBe PrefixTransferSuccess(objectCount)

              // TODO: Check only the prefixed objects were copied correctly
            }
          }
        }
      }
    }

    it("fails if the listing includes an item that doesn't exist") {
      val actualLocationCount = 25

      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocations = (1 to actualLocationCount).map { i =>
          createSrcLocationFrom(srcPrefix, suffix = s"$i.txt")
        }

        withContext { implicit context =>
          withSrcStore(initialEntries = srcLocations.map { _ -> createT }.toMap) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withExtraListingTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              val failure = result.left.value.asInstanceOf[PrefixTransferIncomplete]

              failure.successes shouldBe actualLocationCount
              failure.failures shouldBe 1
            }
          }
        }
      }
    }

    it("fails if the underlying transfer is broken") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocation = createSrcLocationFrom(srcPrefix, suffix = "1.txt")

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(srcLocation -> createT)) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withBrokenTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              result.left.value shouldBe a[PrefixTransferIncomplete]
            }
          }
        }
      }
    }

    it("fails if the underlying listing is broken") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val srcLocation = createSrcLocationFrom(srcPrefix, suffix = "1.txt")

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(srcLocation -> createT)) { srcStore =>
            withDstStore(initialEntries = Map.empty) { dstStore =>
              val result =
                withBrokenListingTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              result.left.value shouldBe a[PrefixTransferListingFailure[_]]
            }
          }
        }
      }
    }

    it("fails if you try to overwrite an existing object") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val src = createSrcLocationFrom(srcPrefix, suffix = "1.txt")
        val dst = createDstLocationFrom(dstPrefix, suffix = "1.txt")

        val srcT = createT
        val dstT = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> srcT)) { srcStore =>
            withDstStore(initialEntries = Map(dst -> dstT)) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix)
                }

              val failure = result.left.value.asInstanceOf[PrefixTransferIncomplete]

              failure.successes shouldBe 0
              failure.failures shouldBe 1

              srcStore.get(src).right.value shouldBe Identified(src, srcT)
              dstStore.get(dst).right.value shouldBe Identified(dst, dstT)
            }
          }
        }
      }
    }

    it("overwrites an existing object if checkForExisting=false") {
      withNamespacePair { case (srcNamespace, dstNamespace) =>
        val srcPrefix = createSrcPrefix(srcNamespace)
        val dstPrefix = createDstPrefix(dstNamespace)

        val src = createSrcLocationFrom(srcPrefix, suffix = "1.txt")
        val dst = createDstLocationFrom(dstPrefix, suffix = "1.txt")

        val srcT = createT
        val dstT = createT

        withContext { implicit context =>
          withSrcStore(initialEntries = Map(src -> srcT)) { srcStore =>
            withDstStore(initialEntries = Map(dst -> dstT)) { dstStore =>
              val result =
                withPrefixTransfer(srcStore, dstStore) {
                  _.transferPrefix(srcPrefix = srcPrefix, dstPrefix = dstPrefix, checkForExisting = false)
                }

              result.right.value shouldBe PrefixTransferSuccess(1)

              srcStore.get(src).right.value shouldBe Identified(src, srcT)
              dstStore.get(dst).right.value shouldBe Identified(dst, srcT)
            }
          }
        }
      }
    }
  }
}
