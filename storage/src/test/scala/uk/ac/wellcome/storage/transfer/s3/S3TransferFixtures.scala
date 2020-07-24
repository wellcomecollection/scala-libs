package uk.ac.wellcome.storage.transfer.s3

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.s3.{S3TypedStore, S3TypedStoreFixtures}
import uk.ac.wellcome.storage.transfer.Transfer
import uk.ac.wellcome.storage.transfer.fixtures.TransferFixtures

trait S3TransferFixtures[T]
    extends TransferFixtures[
      S3ObjectLocation,
      T,
      S3TypedStore[T]]
    with S3TypedStoreFixtures[T] {
  override def withTransferStore[R](
    initialEntries: Map[S3ObjectLocation, T])(
    testWith: TestWith[S3TypedStore[T], R]): R =
    withTypedStoreImpl(storeContext = (), initialEntries = initialEntries.map { case (loc, t) => (loc.toObjectLocation, t) }) {
      typedStore =>
        testWith(typedStore)
    }

  override def withTransfer[R](
    testWith: TestWith[Transfer[S3ObjectLocation, S3ObjectLocation], R])(
    implicit store: S3TypedStore[T]): R =
    testWith(new S3Transfer())

  def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  def withDstNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  def createSrcLocation(srcBucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(srcBucket)

  def createDstLocation(dstBucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(dstBucket)

  def withSrcStore[R](initialEntries: Map[S3ObjectLocation, T])(testWith: TestWith[S3TypedStore[T], R])(implicit context: Unit): R =
    withTypedStoreImpl(context, initialEntries = initialEntries.map { case (loc, t) => (loc.toObjectLocation, t) }) { typedStore =>
      testWith(typedStore)
    }

  def withDstStore[R](initialEntries: Map[S3ObjectLocation, T])(testWith: TestWith[S3TypedStore[T], R])(implicit context: Unit): R =
    withTypedStoreImpl(context, initialEntries = initialEntries.map { case (loc, t) => (loc.toObjectLocation, t) }) { typedStore =>
      testWith(typedStore)
    }
}
