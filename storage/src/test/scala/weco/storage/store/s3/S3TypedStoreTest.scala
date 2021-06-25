package weco.storage.store.s3

import java.io.InputStream

import weco.fixtures.TestWith
import weco.storage._
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.TypedStoreTestCases
import weco.storage.store.fixtures.S3NamespaceFixtures
import weco.storage.streaming.InputStreamWithLength

class S3TypedStoreTest
    extends TypedStoreTestCases[
      S3ObjectLocation,
      Record,
      Bucket,
      S3StreamStore,
      S3TypedStore[Record],
      Unit]
    with S3TypedStoreFixtures[Record]
    with RecordGenerators
    with S3NamespaceFixtures {
  override def withBrokenStreamStore[R](
    testWith: TestWith[S3StreamStore, R]): R = {
    val brokenS3StreamStore = new S3StreamStore {
      override def get(location: S3ObjectLocation): ReadEither = Left(
        StoreReadError(new Throwable("get: BOOM!"))
      )

      override def put(location: S3ObjectLocation)(
        inputStream: InputStreamWithLength): WriteEither = Left(
        StoreWriteError(
          new Throwable("put: BOOM!")
        )
      )
    }

    testWith(brokenS3StreamStore)
  }

  override def withSingleValueStreamStore[R](rawStream: InputStream)(
    testWith: TestWith[S3StreamStore, R]): R = {
    val s3StreamStore: S3StreamStore = new S3StreamStore() {
      override def get(location: S3ObjectLocation): ReadEither =
        Right(
          Identified(
            location,
            new InputStreamWithLength(rawStream, length = 0)
          )
        )
    }

    testWith(s3StreamStore)
  }

  override def createT: Record =
    createRecord

  describe("S3TypedStore") {
    it("errors if the object key is too long") {
      withLocalS3Bucket { bucket =>
        // Maximum length of an s3 key is 1024 bytes as of 25/06/2019
        // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
        val location = S3ObjectLocation(
          bucket = bucket.name,
          key = randomStringOfByteLength(1025)
        )

        val entry = createT

        withStoreImpl(initialEntries = Map.empty) { store =>
          val value = store.put(location)(entry).left.value

          value shouldBe a[InvalidIdentifierFailure]
          value.e.getMessage should startWith(
            "S3 object key byte length is too big")
        }
      }
    }
  }
}
