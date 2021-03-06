package weco.storage.store.s3

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.store.StreamStoreTestCases
import weco.storage._
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.fixtures.S3NamespaceFixtures

class S3StreamStoreTest
    extends StreamStoreTestCases[S3ObjectLocation, Bucket, S3StreamStore, Unit]
    with S3StreamStoreFixtures
    with S3NamespaceFixtures {
  describe("handles errors from S3") {
    describe("get") {
      it("errors if S3 has a problem") {
        val store = new S3StreamStore()(brokenS3Client)

        val result = store.get(createS3ObjectLocation).left.value
        result shouldBe a[StoreReadError]

        val err = result.e
        err shouldBe a[SdkClientException]
        err.getMessage should startWith("Unable to execute HTTP request")
      }

      it("errors if the key doesn't exist") {
        withLocalS3Bucket { bucket =>
          val location = createS3ObjectLocationWith(bucket)
          withStoreImpl(initialEntries = Map.empty) { store =>
            val err = store.get(location).left.value
            err shouldBe a[DoesNotExistError]

            err.e shouldBe a[AmazonS3Exception]
            err.e.getMessage should startWith(
              "The specified key does not exist")
          }
        }
      }

      it("errors if the bucket doesn't exist") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val err =
            store.get(createS3ObjectLocationWith(createBucket)).left.value
          err shouldBe a[DoesNotExistError]

          err.e shouldBe a[AmazonS3Exception]
          err.e.getMessage should startWith(
            "The specified bucket does not exist")
        }
      }

      it("errors if asked to get from an invalid bucket") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val invalidLocation = createS3ObjectLocationWith(createInvalidBucket)
          val err = store.get(invalidLocation).left.value
          err shouldBe a[StoreReadError]

          err.e shouldBe a[AmazonS3Exception]
          err.e.getMessage should startWith(
            "The specified bucket is not valid")
        }
      }
    }

    describe("put") {
      it("errors if S3 fails to respond") {
        val store = new S3StreamStore()(brokenS3Client)

        val result = store.put(createS3ObjectLocation)(createT).left.value
        result shouldBe a[StoreWriteError]

        val err = result.e
        err shouldBe a[SdkClientException]
        err.getMessage should startWith("Unable to execute HTTP request")
      }

      it("errors if the bucket doesn't exist") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val result = store.put(createS3ObjectLocation)(createT).left.value

          result shouldBe a[StoreWriteError]

          val err = result.e
          err shouldBe a[AmazonS3Exception]
          err.getMessage should startWith("The specified bucket does not exist")
        }
      }

      it("errors if the bucket name is invalid") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val result = store.put(createS3ObjectLocationWith(createInvalidBucket))(createT).left.value

          result shouldBe a[StoreWriteError]

          val err = result.e
          err shouldBe a[AmazonS3Exception]
          err.getMessage should startWith("The specified bucket is not valid")
        }
      }

      it("errors if the object key is too long") {
        withLocalS3Bucket { bucket =>
          // Maximum length of an s3 key is 1024 bytes as of 25/06/2019
          // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
          val id = S3ObjectLocation(
            bucket = bucket.name,
            key = randomStringOfByteLength(1025)
          )

          val entry = ReplayableStream(randomBytes())

          withStoreImpl(initialEntries = Map.empty) { store =>
            val value = store.put(id)(entry).left.value

            value shouldBe a[InvalidIdentifierFailure]
            value.e.getMessage should startWith(
              "S3 object key byte length is too big")
          }
        }
      }
    }
  }
}
