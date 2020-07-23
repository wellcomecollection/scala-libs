package uk.ac.wellcome.storage.transfer.memory

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.generators.{ObjectLocationGenerators, Record, RecordGenerators}
import uk.ac.wellcome.storage.{ListingFailure, ObjectLocation, ObjectLocationPrefix}
import uk.ac.wellcome.storage.store.memory.MemoryStore
import uk.ac.wellcome.storage.transfer._

class MemoryPrefixTransferTest
    extends PrefixTransferTestCases[
      ObjectLocation,
      ObjectLocationPrefix,
      Record,
      String,
      String,
      MemoryStore[ObjectLocation, Record] with MemoryPrefixTransfer[ObjectLocation, ObjectLocationPrefix, Record],
      MemoryStore[ObjectLocation, Record] with MemoryPrefixTransfer[ObjectLocation, ObjectLocationPrefix, Record],
      MemoryStore[ObjectLocation, Record] with MemoryPrefixTransfer[ObjectLocation, ObjectLocationPrefix, Record]]
  with RecordGenerators
  with ObjectLocationGenerators {

  type MemoryRecordStore =
    MemoryStore[ObjectLocation, Record] with MemoryPrefixTransfer[ObjectLocation, ObjectLocationPrefix, Record]

  override def withSrcNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric)

  override def withDstNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric)

  override def createSrcLocation(srcNamespace: String): ObjectLocation =
    createObjectLocationWith(srcNamespace)

  override def createDstLocation(dstNamespace: String): ObjectLocation =
    createObjectLocationWith(dstNamespace)

  override def createSrcPrefix(srcNamespace: String): ObjectLocationPrefix =
    createObjectLocationPrefixWith(srcNamespace)

  override def createDstPrefix(dstNamespace: String): ObjectLocationPrefix =
    createObjectLocationPrefixWith(dstNamespace)

  override def createSrcLocationFrom(srcPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    srcPrefix.asLocation(suffix)

  override def createDstLocationFrom(dstPrefix: ObjectLocationPrefix, suffix: String): ObjectLocation =
    dstPrefix.asLocation(suffix)

  override def withSrcStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[MemoryRecordStore, R])(implicit underlying: MemoryRecordStore): R = {
    initialEntries.foreach { case (location, record) =>
      underlying.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(underlying)
  }

  override def withDstStore[R](initialEntries: Map[ObjectLocation, Record])(testWith: TestWith[MemoryRecordStore, R])(implicit underlying: MemoryRecordStore): R = {
    initialEntries.foreach { case (location, record) =>
      underlying.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(underlying)
  }

  class MemoryObjectLocationPrefixTransfer(initialEntries: Map[ObjectLocation, Record])
    extends MemoryStore[ObjectLocation, Record](initialEntries = initialEntries)
      with MemoryPrefixTransfer[ObjectLocation, ObjectLocationPrefix, Record]
      with ObjectLocationPrefixTransfer {
    override protected def startsWith(location: ObjectLocation, prefix: ObjectLocationPrefix): Boolean = {
      location.namespace == prefix.namespace && location.path.startsWith(prefix.path)
    }
  }

  override def withPrefixTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R =
    testWith(srcStore)

  override def withExtraListingTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R = {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def list(prefix: ObjectLocationPrefix): ListingResult = {
        val matchingLocations = entries
          .filter { case (location, _) => startsWith(location, prefix) }
          .map { case (location, _) => location }

        Right(matchingLocations ++ Seq(createObjectLocation))
      }
    }

    testWith(prefixTransfer)
  }

  override def withBrokenListingTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R = {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def list(prefix: ObjectLocationPrefix): ListingResult =
        Left(ListingFailure(prefix))
    }

    testWith(prefixTransfer)
  }

  override def withBrokenTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[PrefixTransfer[ObjectLocationPrefix, ObjectLocation, ObjectLocationPrefix, ObjectLocation], R]): R =  {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def transfer(src: ObjectLocation, dst: ObjectLocation, checkForExisting: Boolean = true): TransferEither =
        Left(TransferSourceFailure(src, dst))
    }

    testWith(prefixTransfer)
  }

  override def withContext[R](testWith: TestWith[MemoryRecordStore, R]): R =
    testWith(
      new MemoryObjectLocationPrefixTransfer(initialEntries = Map.empty)
    )

  override def createT: Record = createRecord
}
