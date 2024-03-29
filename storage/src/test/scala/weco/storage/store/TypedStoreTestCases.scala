package weco.storage.store

import java.io.{FilterInputStream, InputStream}
import weco.fixtures.{RandomGenerators, TestWith}
import weco.storage._
import weco.storage.store.fixtures.TypedStoreFixtures
import weco.storage.streaming.Codec._
import weco.storage.streaming.{Codec, InputStreamWithLength}
import weco.storage.{
  CannotCloseStreamError,
  DecoderError,
  EncoderError,
  JsonEncodingError,
  StoreReadError,
  StoreWriteError
}
import weco.storage.store.fixtures.TypedStoreFixtures
import weco.storage.streaming.{Codec, InputStreamWithLength}

trait TypedStoreTestCases[Ident,
                          T,
                          Namespace,
                          StreamStoreImpl <: StreamStore[Ident],
                          TypedStoreImpl <: TypedStore[Ident, T],
                          StreamStoreContext]
    extends StoreWithOverwritesTestCases[
      Ident,
      T,
      Namespace,
      StreamStoreContext]
    with TypedStoreFixtures[
      Ident,
      T,
      StreamStoreImpl,
      TypedStoreImpl,
      StreamStoreContext]
    with RandomGenerators {

  override def withStoreImpl[R](
    initialEntries: Map[Ident, T],
    storeContext: StreamStoreContext)(testWith: TestWith[StoreImpl, R]): R =
    withTypedStoreImpl(storeContext, initialEntries) { typedStore =>
      testWith(typedStore)
    }

  override def withStoreContext[R](
    testWith: TestWith[StreamStoreContext, R]): R =
    withStreamStoreContext { context =>
      testWith(context)
    }

  def withBrokenStreamStore[R](testWith: TestWith[StreamStoreImpl, R]): R

  class CloseDetectionStream(bytes: Array[Byte])
      extends FilterInputStream(bytesCodec.toStream(bytes).value) {
    var isClosed = false

    override def close(): Unit = {
      isClosed = true
      super.close()
    }
  }

  def withSingleValueStreamStore[R](rawStream: InputStream)(
    testWith: TestWith[StreamStoreImpl, R]): R

  describe("it behaves as a TypedStore") {
    describe("get") {
      it("errors if the streaming store has an error") {
        withNamespace { implicit namespace =>
          withBrokenStreamStore { brokenStreamStore =>
            withTypedStore(brokenStreamStore, initialEntries = Map.empty) {
              typedStore =>
                val result = typedStore.get(createId).left.value

                result shouldBe a[StoreReadError]
            }
          }
        }
      }

      it("always closes the underlying stream") {
        withNamespace { implicit namespace =>
          val closeDetectionStream = new CloseDetectionStream(randomBytes())

          withSingleValueStreamStore(closeDetectionStream) { streamingStore =>
            withTypedStore(streamingStore, initialEntries = Map.empty) {
              typedStore =>
                typedStore.get(createId)

                closeDetectionStream.isClosed shouldBe true
            }
          }
        }
      }

      it("errors if it can't close the stream") {
        withNamespace { implicit namespace =>
          val exception = new Throwable("BOOM!")

          val rawStream = new CloseDetectionStream(randomBytes())

          val closeShieldStream =
            new FilterInputStream(rawStream) {
              override def close(): Unit = throw exception
            }

          withSingleValueStreamStore(closeShieldStream) { streamingStore =>
            withTypedStore(streamingStore, initialEntries = Map.empty) {
              typedStore =>
                val result = typedStore.get(createId).left.value
                result shouldBe a[CannotCloseStreamError]
                result.e shouldBe exception
            }
          }
        }
      }
    }

    describe("put") {
      it("errors if the stream store has an error") {
        withNamespace { implicit namespace =>
          withBrokenStreamStore { implicit brokenStreamStore =>
            withTypedStore(brokenStreamStore, initialEntries = Map.empty) {
              typedStore =>
                val result = typedStore.put(createId)(createT).left.value

                result shouldBe a[StoreWriteError]
            }
          }
        }
      }

      it("errors if the data in the stream store is the wrong format") {
        withNamespace { implicit namespace =>
          val stream = stringCodec.toStream("Not a JSON string").value

          withSingleValueStreamStore(stream) { streamStore =>
            withTypedStore(streamStore, initialEntries = Map.empty) {
              typedStore =>
                val result = typedStore.get(createId).left.value

                result shouldBe a[DecoderError]
            }
          }
        }
      }

      it("errors if the codec can't create a stream") {
        withStoreContext { storeContext =>
          withNamespace { implicit namespace =>
            val exception = new Throwable("BOOM!")

            implicit val brokenCodec: Codec[T] = new Codec[T] {
              override def toStream(
                t: T): Either[EncoderError, InputStreamWithLength] =
                Left(JsonEncodingError(exception))

              override def fromStream(
                inputStream: InputStream): Either[DecoderError, T] =
                Left(JsonDecodingError(exception))
            }

            withTypedStoreImpl(storeContext, initialEntries = Map.empty) {
              typedStore =>
                val result = typedStore.put(createId)(createT).left.value

                result shouldBe a[JsonEncodingError]
            }(brokenCodec)
          }
        }
      }
    }
  }
}
