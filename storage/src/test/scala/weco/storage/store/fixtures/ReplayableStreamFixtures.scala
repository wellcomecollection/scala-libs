package weco.storage.store.fixtures

import org.scalatest.EitherValues
import weco.fixtures.RandomGenerators
import weco.storage.streaming.Codec.bytesCodec
import weco.storage.streaming.InputStreamWithLength

trait ReplayableStreamFixtures extends EitherValues with RandomGenerators {
  // In the StoreTestCases, we need to assert that PUT and then GET returns an equivalent
  // value.  A regular InputStream gets consumed on the initial PUT, so we wrap it in
  // a ReplayableStream so we can do comparisons later.

  class ReplayableStream(val originalBytes: Array[Byte], length: Long)
      extends InputStreamWithLength(
        inputStream = bytesCodec.toStream(originalBytes).value,
        length = length
      )

  object ReplayableStream {
    def apply(bytes: Array[Byte]): ReplayableStream =
      new ReplayableStream(bytes, length = bytes.length)
  }

  def createReplayableStream: ReplayableStream =
    ReplayableStream(randomBytes())
}
