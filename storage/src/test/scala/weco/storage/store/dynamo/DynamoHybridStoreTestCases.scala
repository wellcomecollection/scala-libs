package weco.storage.store.dynamo

import org.scanamo.generic.auto._
import weco.fixtures.TestWith
import weco.storage._
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.fixtures.{DynamoFixtures, S3Fixtures}
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.store._
import weco.storage.store.s3.{S3StreamStore, S3TypedStore}
import weco.storage.{
  DanglingHybridStorePointerError,
  InvalidIdentifierFailure,
  StoreReadError,
  StoreWriteError
}
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.fixtures.{DynamoFixtures, S3Fixtures}
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.store.HybridStoreWithoutOverwritesTestCases
import weco.storage.store.s3.{S3StreamStore, S3TypedStore}

import scala.language.higherKinds

trait DynamoHybridStoreTestCases[
  DynamoStoreImpl <: Store[Version[String, Int], S3ObjectLocation]]
    extends HybridStoreWithoutOverwritesTestCases[
      Version[String, Int],
      S3ObjectLocation,
      Record,
      Unit,
      S3TypedStore[Record],
      DynamoStoreImpl,
      (Bucket, Table)
    ]
    with RecordGenerators
    with S3Fixtures
    with DynamoFixtures {
  type S3TypedStoreImpl = S3TypedStore[Record]
  type DynamoIndexedStoreImpl = DynamoStoreImpl

  def createPrefix(
    implicit context: (Bucket, Table)): S3ObjectLocationPrefix = {
    val (bucket, _) = context
    createS3ObjectLocationPrefixWith(bucket)
  }

  override def withTypedStoreImpl[R](testWith: TestWith[S3TypedStoreImpl, R])(
    implicit context: (Bucket, Table)): R =
    testWith(S3TypedStore[Record])

  override def createTypedStoreId(implicit context: Unit): S3ObjectLocation =
    createS3ObjectLocation

  override def withBrokenPutTypedStoreImpl[R](
    testWith: TestWith[S3TypedStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    val s3StreamStore: S3StreamStore = new S3StreamStore()

    testWith(
      new S3TypedStore[Record]()(codec, s3StreamStore) {
        override def put(location: S3ObjectLocation)(
          entry: Record): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetTypedStoreImpl[R](
    testWith: TestWith[S3TypedStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    val s3StreamStore: S3StreamStore = new S3StreamStore()

    testWith(
      new S3TypedStore[Record]()(codec, s3StreamStore) {
        override def get(location: S3ObjectLocation): ReadEither =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }

  override def withStoreContext[R](testWith: TestWith[(Bucket, Table), R]): R =
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        testWith((bucket, table))
      }
    }

  override def createT: Record = createRecord

  override def withNamespace[R](testWith: TestWith[Unit, R]): R =
    testWith(())

  override def createId(implicit namespace: Unit): Version[String, Int] =
    Version(id = randomAlphanumeric(), version = randomInt(from = 1, to = 10))

  describe("DynamoHybridStore") {
    it("appends a .json suffix to object keys") {
      withStoreContext { implicit context =>
        withNamespace { implicit namespace =>
          withTypedStoreImpl { typedStore =>
            withIndexedStoreImpl { indexedStore =>
              withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                val id = createId
                val hybridStoreEntry = createT

                val putResult = hybridStore.put(id)(hybridStoreEntry)
                val putValue = putResult.value

                val dynamoResult = indexedStore.get(putValue.id)
                val dynamoValue = dynamoResult.value

                val s3Location = dynamoValue.identifiedT

                s3Location.key should endWith(".json")
              }
            }
          }
        }
      }
    }

    describe("it handles errors from AWS") {
      it("if the prefix refers to a non-existent bucket") {
        withStoreContext {
          case (_, table) =>
            val nonExistentBucket = createBucket

            implicit val context = (nonExistentBucket, table)

            withNamespace { implicit namespace =>
              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId
                    val hybridStoreEntry = createT

                    val result = hybridStore.put(id)(hybridStoreEntry)
                    val value = result.left.value

                    value shouldBe a[StoreWriteError]
                    value.e.getMessage should startWith(
                      "The specified bucket does not exist")
                  }
                }
              }
            }
        }
      }

      it("if the prefix refers to an invalid bucket name") {
        withStoreContext {
          case (_, table) =>
            val invalidBucket = Bucket(createInvalidBucketName)

            implicit val context = (invalidBucket, table)

            withNamespace { implicit namespace =>
              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId
                    val hybridStoreEntry = createT

                    val result = hybridStore.put(id)(hybridStoreEntry)
                    val value = result.left.value

                    value shouldBe a[StoreWriteError]
                    value.e.getMessage should startWith(
                      "The specified bucket is not valid")
                  }
                }
              }
            }
        }
      }

      it("if the prefix creates an S3 key that's too long") {
        withStoreContext { implicit context =>
          withTypedStoreImpl { typedStore =>
            withIndexedStoreImpl { indexedStore =>
              withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                // Maximum length of an s3 key is 1024 bytes as of 25/06/2019
                // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html

                // The hybrid store appends _some_ value to this path.
                // This test sets the id length to 1024 expecting the
                // implementation to append >0 bytes thereby causing
                // a failure.

                // There is also a dynamo hash/range key restriction but
                // this is (at time of writing) greater than the s3 key
                // length restriction and cannot be reached without
                // invoking this error.

                val tooLongId = randomStringOfByteLength(1024)

                val id = Version(id = tooLongId, version = 1)
                val hybridStoreEntry = createT

                val result = hybridStore.put(id)(hybridStoreEntry)
                val value = result.left.value

                value shouldBe a[InvalidIdentifierFailure]
                value.e.getMessage should startWith(
                  "S3 object key byte length is too big")
              }
            }
          }
        }
      }

      it("if the underlying DynamoDB table doesn't exist") {
        withStoreContext {
          case (bucket, _) =>
            implicit val context = (bucket, nonExistentTable)

            withNamespace { implicit namespace =>
              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId
                    val hybridStoreEntry = createT

                    val result = hybridStore.put(id)(hybridStoreEntry)
                    val value = result.left.value

                    value shouldBe a[StoreWriteError]
                    value.e.getMessage should startWith(
                      "Cannot do operations on a non-existent table")
                  }
                }
              }

            }
        }
      }

      it("if a DynamoDB index entry points to a non-existent S3 key") {
        withStoreContext {
          case (bucket, table) =>
            withNamespace { implicit namespace =>
              implicit val context = (bucket, table)

              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId
                    val hybridStoreEntry = createT

                    hybridStore.put(id)(hybridStoreEntry) shouldBe a[Right[_,
                                                                           _]]

                    val indexedEntry = indexedStore.get(id).value
                    val s3Location = indexedEntry.identifiedT

                    s3Client.deleteObject(s3Location.bucket, s3Location.key)

                    val value = hybridStore.get(id).left.value

                    value shouldBe a[DanglingHybridStorePointerError]
                  }
                }
              }
            }
        }
      }

      it("if a DynamoDB index entry points to a non-existent S3 bucket") {
        withStoreContext {
          case (bucket, table) =>
            withNamespace { implicit namespace =>
              implicit val context = (bucket, table)

              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId
                    val hybridStoreEntry = createT

                    hybridStore.put(id)(hybridStoreEntry) shouldBe a[Right[_,
                                                                           _]]

                    val indexedEntry = indexedStore.get(id).value
                    val s3Location = indexedEntry.identifiedT

                    s3Client.deleteObject(s3Location.bucket, s3Location.key)
                    s3Client.deleteBucket(s3Location.bucket)

                    val value = hybridStore.get(id).left.value

                    value shouldBe a[DanglingHybridStorePointerError]
                    value.e.getMessage should startWith(
                      "The specified bucket does not exist")
                  }
                }
              }
            }
        }
      }

      it("if the DynamoDB row is in the wrong format") {
        withStoreContext {
          case (bucket, table) =>
            withNamespace { implicit namespace =>
              implicit val context = (bucket, table)

              withTypedStoreImpl { typedStore =>
                withIndexedStoreImpl { indexedStore =>
                  withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                    val id = createId

                    case class BadRow(id: String,
                                      version: Int,
                                      contents: String)

                    putTableItem(
                      item = BadRow(id.id, id.version, randomAlphanumeric()),
                      table = table
                    )

                    val value = hybridStore.get(id).left.value

                    value shouldBe a[StoreReadError]
                    value.e.getMessage should startWith("DynamoReadError")
                  }
                }
              }
            }
        }
      }
    }
  }
}
