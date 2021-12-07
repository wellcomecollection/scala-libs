package weco.storage.store.s3

import weco.fixtures.TestWith
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.fixtures.TypedStoreFixtures
import weco.storage.streaming.Codec

trait S3TypedStoreFixtures[T]
    extends TypedStoreFixtures[
      S3ObjectLocation,
      T,
      S3StreamStore,
      S3TypedStore[T],
      Unit]
    with S3StreamStoreFixtures {
  override def withTypedStore[R](streamStore: S3StreamStore,
                                 initialEntries: Map[S3ObjectLocation, T])(
    testWith: TestWith[S3TypedStore[T], R])(implicit codec: Codec[T]): R = {
    implicit val s3StreamStore: S3StreamStore = streamStore

    initialEntries.foreach {
      case (location, t) =>
        val stream = codec.toStream(t).value

        putStream(location, stream)
    }

    testWith(new S3TypedStore[T]())
  }
}
