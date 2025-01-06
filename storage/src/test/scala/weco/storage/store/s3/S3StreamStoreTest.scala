package weco.storage.store.s3

import java.io.ByteArrayInputStream

import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, S3Exception}
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.store.StreamStoreTestCases
import weco.storage._
import weco.storage.providers.s3.S3ObjectLocation
import weco.storage.store.fixtures.S3NamespaceFixtures
import weco.storage.streaming.InputStreamWithLength

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
        err.getMessage should startWith("Received an UnknownHostException when attempting to interact with a service")
      }

      it("errors if the key doesn't exist") {
        withLocalS3Bucket { bucket =>
          val location = createS3ObjectLocationWith(bucket)
          withStoreImpl(initialEntries = Map.empty) { store =>
            val err = store.get(location).left.value
            err shouldBe a[DoesNotExistError]

            err.e shouldBe a[S3Exception]
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

          err.e shouldBe a[S3Exception]
          err.e.getMessage should startWith(
            "The specified bucket does not exist")
        }
      }

      it("errors if asked to get from an invalid bucket") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val invalidLocation = createS3ObjectLocationWith(createInvalidBucket)
          val err = store.get(invalidLocation).left.value
          err shouldBe a[DoesNotExistError]

          err.e shouldBe a[S3Exception]
          err.e.getMessage should startWith(
            "The specified bucket does not exist")
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
        err.getCause.getMessage should startWith("Unable to execute HTTP request")
      }

      it("errors if the bucket doesn't exist") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val result = store.put(createS3ObjectLocation)(createT).left.value

          result shouldBe a[StoreWriteError]

          val err = result.e
          err shouldBe a[S3Exception]
          err.getMessage should startWith("The specified bucket does not exist")
        }
      }

      it("errors if the bucket name is invalid") {
        withStoreImpl(initialEntries = Map.empty) { store =>
          val result = store
            .put(createS3ObjectLocationWith(createInvalidBucket))(createT)
            .left
            .value

          result shouldBe a[StoreWriteError]

          val err = result.e
          err shouldBe a[S3Exception]
          err.getMessage should startWith("The specified bucket does not exist")
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
            value.e.getMessage should include("key is too long")
          }
        }
      }
    }
  }

  it("uploads a large file (>partSize)") {
    val length = (10 * FileUtils.ONE_MB).toInt + 1

    val bytes = randomBytes(length)
    val inputStream = new ByteArrayInputStream(bytes.toArray)
    val store = new S3StreamStore(partSize = length - 1)

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)

      val result = store.put(location)(new InputStreamWithLength(inputStream, length))
      result shouldBe a[Right[_, _]]

      val getRequest =
        GetObjectRequest.builder()
          .bucket(location.bucket)
          .key(location.key)
          .build()

      s3Client.getObjectAsBytes(getRequest).asByteArray() shouldBe bytes
    }
  }
}
