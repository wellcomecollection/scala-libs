package uk.ac.wellcome.storage.store.memory

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.StreamStore
import uk.ac.wellcome.storage.streaming.Codec._
import uk.ac.wellcome.storage.streaming.InputStreamWithLengthAndMetadata

class MemoryStreamStore[Ident](
  val memoryStore: MemoryStore[Ident, MemoryStreamStoreEntry])
    extends StreamStore[Ident, InputStreamWithLengthAndMetadata]
    with Logging {
  override def get(id: Ident): ReadEither =
    for {
      entry <- memoryStore.get(id)
      internalEntry = entry.identifiedT

      // This is sort of cheating, but since we created these streams the lengths
      // should never be incorrect, and it means we can't get an EncoderError
      // (a WriteError) from inside a REad method.
      inputStream = bytesCodec.toStream(internalEntry.bytes).right.get

      result = InputStreamWithLengthAndMetadata(
        inputStream,
        internalEntry.metadata)
    } yield Identified(id, result)

  override def put(id: Ident)(
    entry: InputStreamWithLengthAndMetadata): WriteEither =
    bytesCodec.fromStream(entry) match {
      case Right(bytes) =>
        val internalEntry = MemoryStreamStoreEntry(bytes, metadata = entry.metadata)
        memoryStore.put(id)(internalEntry).map { _ =>
          Identified(id, entry)
        }

      case Left(err: IncorrectStreamLengthError) => Left(err)

      case Left(err: DecoderError) => Left(StoreWriteError(err.e))
    }
}

object MemoryStreamStore {
  def apply[Ident](): MemoryStreamStore[Ident] = {
    val memoryStore = new MemoryStore[Ident, MemoryStreamStoreEntry](initialEntries = Map.empty)

    new MemoryStreamStore[Ident](memoryStore)
  }
}

case class MemoryStreamStoreEntry(
  bytes: Array[Byte],
  metadata: Map[String, String]
)
