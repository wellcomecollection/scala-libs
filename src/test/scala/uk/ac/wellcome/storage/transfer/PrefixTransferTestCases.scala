package uk.ac.wellcome.storage.transfer

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FunSpec, Matchers}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.Identified
import uk.ac.wellcome.storage.store.Store
import uk.ac.wellcome.storage.store.fixtures.NamespaceFixtures

trait PrefixTransferTestCases[Location, Prefix, Namespace, T, StoreImpl <: Store[Location, T]]
  extends FunSpec
    with Matchers
    with EitherValues
    with NamespaceFixtures[Location, Namespace]
    with ScalaFutures {
  def withPrefixTransferStore[R](initialEntries: Map[Location, T])(testWith: TestWith[StoreImpl, R]): R

  def withPrefixTransfer[R](testWith: TestWith[PrefixTransfer[Prefix, Location], R])(implicit store: StoreImpl): R

  def withExtraListingTransfer[R](testWith: TestWith[PrefixTransfer[Prefix, Location], R])(implicit store: StoreImpl): R
  def withBrokenListingTransfer[R](testWith: TestWith[PrefixTransfer[Prefix, Location], R])(implicit store: StoreImpl): R
  def withBrokenTransfer[R](testWith: TestWith[PrefixTransfer[Prefix, Location], R])(implicit store: StoreImpl): R

  def createPrefix(implicit namespace: Namespace): Prefix

  def createLocationFrom(prefix: Prefix, suffix: String): Location

  def createT: T

  describe("behaves as a PrefixTransfer") {
    it("does nothing if the prefix is empty") {
      withNamespace { implicit namespace =>
        withPrefixTransferStore(initialEntries = Map.empty) { implicit store =>
          withPrefixTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = createPrefix,
              dstPrefix = createPrefix
            )

            whenReady(future) {
              _.right.value shouldBe PrefixTransferSuccess(Seq.empty)
            }
          }
        }
      }
    }

    it("copies a single item") {
      withNamespace { implicit namespace =>
        val srcPrefix = createPrefix
        val dstPrefix = createPrefix

        val srcLocation = createLocationFrom(srcPrefix, suffix = "1.txt")
        val dstLocation = createLocationFrom(dstPrefix, suffix = "1.txt")

        val t = createT

        withPrefixTransferStore(initialEntries = Map(srcLocation -> t)) { implicit store =>
          withPrefixTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = srcPrefix,
              dstPrefix = dstPrefix
            )

            whenReady(future) {
              _.right.value shouldBe PrefixTransferSuccess(
                Seq(TransferPerformed(srcLocation, dstLocation))
              )
            }


            store.get(srcLocation).right.value shouldBe Identified(srcLocation, t)
            store.get(dstLocation).right.value shouldBe Identified(dstLocation, t)
          }
        }
      }
    }

    it("copies multiple items") {
      withNamespace { implicit namespace =>
        val srcPrefix = createPrefix
        val dstPrefix = createPrefix

        val srcLocations = (1 to 5).map { i => createLocationFrom(srcPrefix, suffix = s"$i.txt") }
        val dstLocations = (1 to 5).map { i => createLocationFrom(dstPrefix, suffix = s"$i.txt") }

        val valuesT = (1 to 5).map { _ => createT }

        val expectedResults: Seq[TransferPerformed[Location]] = srcLocations.zip(dstLocations).map { case (src, dst) =>
          TransferPerformed(src, dst)
        }

        withPrefixTransferStore(initialEntries = srcLocations.zip(valuesT).toMap) { implicit store =>
          withPrefixTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = srcPrefix,
              dstPrefix = dstPrefix
            )

            whenReady(future) { result =>
              val transferResult = result.right.value
              transferResult shouldBe a[PrefixTransferSuccess]
              transferResult.asInstanceOf[PrefixTransferSuccess].successes should contain theSameElementsAs expectedResults

              // TODO: I don't think this is doing the right thing!
//              expectedResults.zip(valuesT).map { case (result, t) =>
//                store.get(result.source).right.value shouldBe Identified(result.source, t)
//                store.get(result.destination).right.value shouldBe Identified(result.destination, t)
//              }
            }
          }
        }
      }
    }

    it("does not copy items from outside the prefix") {
      withNamespace { implicit namespace =>
        val srcPrefix = createPrefix
        val dstPrefix = createPrefix

        val srcLocations = (1 to 5).map { i => createLocationFrom(srcPrefix, suffix = s"$i.txt") }
        val dstLocations = (1 to 5).map { i => createLocationFrom(dstPrefix, suffix = s"$i.txt") }

        val valuesT = (1 to 5).map { _ => createT }

        val expectedResults: Seq[TransferPerformed[Location]] = srcLocations.zip(dstLocations).map { case (src, dst) =>
          TransferPerformed(src, dst)
        }

        val otherPrefix = createPrefix
        val otherLocation = createLocationFrom(otherPrefix, suffix = "other.txt")

        val initialEntries = srcLocations.zip(valuesT).toMap ++ Map(otherLocation -> createT)

        withPrefixTransferStore(initialEntries = initialEntries) { implicit store =>
          withPrefixTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = srcPrefix,
              dstPrefix = dstPrefix
            )

            whenReady(future) { result =>
              val transferResult = result.right.value

              transferResult shouldBe a[PrefixTransferSuccess]
              val transferSuccess = transferResult.asInstanceOf[PrefixTransferSuccess]
              transferSuccess.successes should contain theSameElementsAs expectedResults

              // TODO: Revisit this
//              expectedResults.zip(valuesT).map { case (r, t) =>
//                store.get(transferSuccess.source).right.value shouldBe Identified(r.source, t)
//                store.get(transferSuccess.destination).right.value shouldBe Identified(r.destination, t)
//              }
            }
          }
        }
      }
    }

    it("fails if the listing includes an item that doesn't exist") {
      withNamespace { implicit namespace =>
        val srcPrefix = createPrefix

        val srcLocations = (1 to 5).map { i => createLocationFrom(srcPrefix, suffix = s"$i.txt") }

        val valuesT = (1 to 5).map { _ => createT }

        withPrefixTransferStore(initialEntries = srcLocations.zip(valuesT).toMap) { implicit store =>
          withExtraListingTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = srcPrefix,
              dstPrefix = createPrefix
            )

            whenReady(future) { result =>
              val transferResult = result.left.value

              transferResult shouldBe a[PrefixTransferFailure]
              val failure = transferResult.asInstanceOf[PrefixTransferFailure]
              failure.successes.size shouldBe 5
              failure.failures.size shouldBe 1
            }
          }
        }
      }
    }

    it("fails if the underlying transfer is broken") {
      withNamespace { implicit namespace =>
        val srcPrefix = createPrefix
        val srcLocation = createLocationFrom(srcPrefix, suffix = "1.txt")

        val t = createT

        withPrefixTransferStore(initialEntries = Map(srcLocation -> t)) { implicit store =>
          withBrokenTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = srcPrefix,
              dstPrefix = createPrefix
            )

            whenReady(future) {
              _.left.value shouldBe a[PrefixTransferFailure]
            }
          }
        }
      }
    }

    it("fails if the underlying listing is broken") {
      withNamespace { implicit namespace =>
        withPrefixTransferStore(initialEntries = Map.empty) { implicit store =>
          withBrokenListingTransfer { prefixTransfer =>
            val future = prefixTransfer.transferPrefix(
              srcPrefix = createPrefix,
              dstPrefix = createPrefix
            )

            whenReady(future) {
              _.left.value shouldBe a[PrefixTransferListingFailure[_]]
            }
          }
        }
      }
    }
  }
}

