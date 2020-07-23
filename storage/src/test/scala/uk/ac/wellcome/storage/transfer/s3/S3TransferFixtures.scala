package uk.ac.wellcome.storage.transfer.s3

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.s3.{S3StreamStore, S3StreamStoreFixtures, S3TypedStore}
import uk.ac.wellcome.storage.streaming.Codec
import uk.ac.wellcome.storage.transfer.Transfer
import uk.ac.wellcome.storage.transfer.fixtures.TransferFixtures

trait S3TransferFixtures[T]
    extends TransferFixtures[
      S3ObjectLocation,
      T,
      S3TypedStore[T]]
  with S3StreamStoreFixtures {


  // TODO: Bridging code while we split ObjectLocation. Remove this eventually;
  // use S3TypedStoreFixtures.
  def withTypedStore[R](
    streamStore: S3StreamStore,
    initialEntries: Map[S3ObjectLocation, T])(
    testWith: TestWith[S3TypedStore[T], R])(implicit codec: Codec[T]): R = {
    implicit val s3StreamStore: S3StreamStore = streamStore

    initialEntries.foreach {
      case (location, t) =>
        val stream = codec.toStream(t).right.value

        putStream(ObjectLocation(location.bucket, location.key), stream)
    }

    testWith(new S3TypedStore[T]())
  }


  override def withTransferStore[R](
    initialEntries: Map[S3ObjectLocation, T])(
    testWith: TestWith[S3TypedStore[T], R]): R =
    withTypedStoreImpl(storeContext = (), initialEntries = initialEntries) {
      typedStore =>
        testWith(typedStore)
    }

  override def withTransfer[R](
    testWith: TestWith[Transfer[S3ObjectLocation, S3ObjectLocation], R])(
    implicit store: S3TypedStore[T]): R =
    testWith(new S3Transfer())
}
