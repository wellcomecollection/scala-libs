package weco.storage.store.s3

import weco.fixtures.TestWith
import weco.storage.fixtures.S3Fixtures
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.fixtures.StreamStoreFixtures
import weco.storage.streaming.InputStreamWithLength

trait S3StreamStoreFixtures
    extends StreamStoreFixtures[S3ObjectLocation, S3StreamStore, Unit]
    with S3Fixtures {
  override def withStreamStoreImpl[R](
    context: Unit,
    initialEntries: Map[S3ObjectLocation, InputStreamWithLength])(
    testWith: TestWith[S3StreamStore, R]): R = {
    initialEntries.foreach {
      case (location, stream) =>
        putStream(location, stream)
    }

    testWith(new S3StreamStore())
  }

  override def withStreamStoreContext[R](testWith: TestWith[Unit, R]): R =
    testWith(())
}
