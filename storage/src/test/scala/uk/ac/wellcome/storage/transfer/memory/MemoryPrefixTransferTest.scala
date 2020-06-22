package uk.ac.wellcome.storage.transfer.memory

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ListingFailure
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.memory.{MemoryLocation, MemoryLocationPrefix}
import uk.ac.wellcome.storage.store.memory.MemoryStore
import uk.ac.wellcome.storage.transfer._

class MemoryPrefixTransferTest
    extends PrefixTransferTestCases[
      MemoryLocation,
      MemoryLocationPrefix,
      MemoryLocation,
      MemoryLocationPrefix,
      Record,
      String,
      String,
      MemoryStore[MemoryLocation, Record] with MemoryPrefixTransfer[MemoryLocation, MemoryLocationPrefix, Record],
      MemoryStore[MemoryLocation, Record] with MemoryPrefixTransfer[MemoryLocation, MemoryLocationPrefix, Record],
      MemoryStore[MemoryLocation, Record] with MemoryPrefixTransfer[MemoryLocation, MemoryLocationPrefix, Record]]
  with RecordGenerators {

  type MemoryRecordStore =
    MemoryStore[MemoryLocation, Record] with MemoryPrefixTransfer[MemoryLocation, MemoryLocationPrefix, Record]

  override def withSrcNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric)

  override def withDstNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric)

  override def createSrcLocation(srcNamespace: String): MemoryLocation =
    MemoryLocation(srcNamespace, path = randomAlphanumeric)

  override def createDstLocation(dstNamespace: String): MemoryLocation =
    MemoryLocation(dstNamespace, path = randomAlphanumeric)

  override def createSrcPrefix(srcNamespace: String): MemoryLocationPrefix =
    MemoryLocationPrefix(srcNamespace, path = randomAlphanumeric)

  override def createDstPrefix(dstNamespace: String): MemoryLocationPrefix =
    MemoryLocationPrefix(dstNamespace, path = randomAlphanumeric)

  override def createSrcLocationFrom(srcPrefix: MemoryLocationPrefix, suffix: String): MemoryLocation =
    srcPrefix.asLocation(suffix)

  override def createDstLocationFrom(dstPrefix: MemoryLocationPrefix, suffix: String): MemoryLocation =
    dstPrefix.asLocation(suffix)

  override def withSrcStore[R](initialEntries: Map[MemoryLocation, Record])(testWith: TestWith[MemoryRecordStore, R])(implicit underlying: MemoryRecordStore): R = {
    initialEntries.foreach { case (location, record) =>
      underlying.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(underlying)
  }

  override def withDstStore[R](initialEntries: Map[MemoryLocation, Record])(testWith: TestWith[MemoryRecordStore, R])(implicit underlying: MemoryRecordStore): R = {
    initialEntries.foreach { case (location, record) =>
      underlying.put(location)(record) shouldBe a[Right[_, _]]
    }

    testWith(underlying)
  }

  class MemoryObjectLocationPrefixTransfer(initialEntries: Map[MemoryLocation, Record])
    extends MemoryStore[MemoryLocation, Record](initialEntries = initialEntries)
      with MemoryPrefixTransfer[MemoryLocation, MemoryLocationPrefix, Record] {
    override protected def startsWith(location: MemoryLocation, prefix: MemoryLocationPrefix): Boolean = {
      location.namespace == prefix.namespace && location.path.startsWith(prefix.path)
    }

    override protected def buildDstLocation(
      srcPrefix: MemoryLocationPrefix,
      dstPrefix: MemoryLocationPrefix,
      srcLocation: MemoryLocation
   ): MemoryLocation =
      dstPrefix.asLocation(
        srcLocation.path.stripPrefix(srcPrefix.path)
      )
  }

  type TransferImpl =
    PrefixTransfer[
      MemoryLocation,
      MemoryLocationPrefix,
      MemoryLocation,
      MemoryLocationPrefix]

  override def withPrefixTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[TransferImpl, R]): R =
    testWith(srcStore)

  override def withExtraListingTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[TransferImpl, R]): R = {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def list(prefix: MemoryLocationPrefix): ListingResult = {
        val matchingLocations = entries
          .filter { case (location, _) => startsWith(location, prefix) }
          .map { case (location, _) => location }

        Right(matchingLocations ++ Seq(MemoryLocation(randomAlphanumeric, randomAlphanumeric)))
      }
    }

    testWith(prefixTransfer)
  }

  override def withBrokenListingTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[TransferImpl, R]): R = {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def list(prefix: MemoryLocationPrefix): ListingResult =
        Left(ListingFailure(prefix))
    }

    testWith(prefixTransfer)
  }

  override def withBrokenTransfer[R](srcStore: MemoryRecordStore, dstStore: MemoryRecordStore)(testWith: TestWith[TransferImpl, R]): R =  {
    val prefixTransfer = new MemoryObjectLocationPrefixTransfer(initialEntries = srcStore.entries ++ dstStore.entries) {
      override def transfer(src: MemoryLocation, dst: MemoryLocation, checkForExisting: Boolean = true): Either[TransferFailure, TransferSuccess] =
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
