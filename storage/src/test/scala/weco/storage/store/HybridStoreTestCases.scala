package weco.storage.store

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.storage.generators.StreamGenerators
import weco.storage.streaming.InputStreamWithLength
import weco.storage.{
  DanglingHybridStorePointerError,
  DoesNotExistError,
  JsonDecodingError,
  ReadError,
  WriteError
}

trait HybridStoreTestCases[
  IndexedStoreId,
  TypedStoreId,
  T,
  Namespace,
  TypedStoreImpl <: TypedStore[TypedStoreId, T],
  IndexedStoreImpl <: Store[IndexedStoreId, TypedStoreId],
  HybridStoreContext]
    extends AnyFunSpec
    with StoreTestCases[IndexedStoreId, T, Namespace, HybridStoreContext]
    with Matchers
    with StreamGenerators
    with EitherValues {

  type HybridStoreImpl = HybridStore[IndexedStoreId, TypedStoreId, T]

  def withHybridStoreImpl[R](
    typedStore: TypedStoreImpl,
    indexedStore: IndexedStoreImpl)(testWith: TestWith[HybridStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def withTypedStoreImpl[R](testWith: TestWith[TypedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def withIndexedStoreImpl[R](testWith: TestWith[IndexedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def createTypedStoreId(implicit namespace: Namespace): TypedStoreId

  def createIndexedStoreId(implicit namespace: Namespace): IndexedStoreId =
    createId

  def withBrokenPutTypedStoreImpl[R](testWith: TestWith[TypedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def withBrokenGetTypedStoreImpl[R](testWith: TestWith[TypedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def withBrokenPutIndexedStoreImpl[R](testWith: TestWith[IndexedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  def withBrokenGetIndexedStoreImpl[R](testWith: TestWith[IndexedStoreImpl, R])(
    implicit context: HybridStoreContext): R

  override def withStoreImpl[R](
    initialEntries: Map[IndexedStoreId, T],
    storeContext: HybridStoreContext)(testWith: TestWith[StoreImpl, R]): R = {
    implicit val context: HybridStoreContext = storeContext

    withTypedStoreImpl { typedStore =>
      withIndexedStoreImpl { indexedStore =>
        withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
          initialEntries.map {
            case (id, entry) =>
              hybridStore.put(id)(entry) shouldBe a[Right[_, _]]
          }

          testWith(hybridStore)
        }
      }
    }
  }

  describe("it behaves as a HybridStore") {
    describe("storing a new record") {
      it("stores the object in the object store") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withTypedStoreImpl { typedStore =>
              withIndexedStoreImpl { indexedStore =>
                withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                  val id = createId
                  val hybridStoreEntry = createT

                  val putResult = hybridStore.put(id)(hybridStoreEntry)
                  val putValue = putResult.value

                  val indexedResult = indexedStore.get(putValue.id)
                  val indexedValue = indexedResult.value

                  val typedStoreId = indexedValue.identifiedT

                  val typedResult = typedStore.get(typedStoreId)
                  val typedValue = typedResult.value
                  typedValue.identifiedT shouldBe hybridStoreEntry
                }
              }
            }
          }
        }
      }
    }

    describe("handles errors in the underlying stores") {
      it("if the typed store has a write error") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withBrokenPutTypedStoreImpl { typedStore =>
              withIndexedStoreImpl { indexedStore =>
                withHybridStoreImpl(typedStore, indexedStore) {
                  _.put(createId)(createT).left.value shouldBe a[WriteError]
                }
              }
            }
          }
        }
      }

      it("if the indexed store has a write error") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withTypedStoreImpl { typedStore =>
              withBrokenPutIndexedStoreImpl { indexedStore =>
                withHybridStoreImpl(typedStore, indexedStore) {
                  _.put(createId)(createT).left.value shouldBe a[WriteError]
                }
              }
            }
          }
        }
      }

      it("if the indexed store refers to a missing typed store entry") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withTypedStoreImpl { typedStore =>
              withIndexedStoreImpl { indexedStore =>
                val indexedStoreId = createIndexedStoreId
                val typedStoreId = createTypedStoreId

                indexedStore.put(indexedStoreId)(typedStoreId) shouldBe a[
                  Right[_, _]]

                withHybridStoreImpl(typedStore, indexedStore) {
                  _.get(indexedStoreId).left.value shouldBe a[
                    DanglingHybridStorePointerError]
                }
              }
            }
          }
        }
      }

      it("if the typed store has a read error") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withBrokenGetTypedStoreImpl { typedStore =>
              withIndexedStoreImpl { indexedStore =>
                val indexedStoreId = createIndexedStoreId
                val typedStoreId = createTypedStoreId

                indexedStore.put(indexedStoreId)(typedStoreId) shouldBe a[
                  Right[_, _]]

                withHybridStoreImpl(typedStore, indexedStore) { hybridStore =>
                  val err = hybridStore.get(indexedStoreId).left.value
                  err shouldBe a[ReadError]
                  err.isInstanceOf[DoesNotExistError] shouldBe false
                }
              }
            }
          }
        }
      }

      it("if the indexed store has a read error") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withTypedStoreImpl { typedStore =>
              withBrokenGetIndexedStoreImpl { indexedStore =>
                withHybridStoreImpl(typedStore, indexedStore) {
                  _.get(createId).left.value shouldBe a[ReadError]
                }
              }
            }
          }
        }
      }

      it("if the data in the typed store is the wrong format") {
        withStoreContext { implicit context =>
          withNamespace { implicit namespace =>
            withTypedStoreImpl { typedStore =>
              withIndexedStoreImpl { indexedStore =>
                withHybridStoreImpl(typedStore, indexedStore) {
                  hybridStoreImpl =>
                    val id = createId

                    hybridStoreImpl.put(id)(createT) shouldBe a[Right[_, _]]

                    val typeStoreId =
                      indexedStore.get(id).value.identifiedT

                    val byteLength = 256
                    val inputStream = new InputStreamWithLength(
                      createInputStream(byteLength),
                      length = byteLength
                    )

                    typedStore.streamStore.put(typeStoreId)(inputStream) shouldBe a[
                      Right[_, _]]

                    val value = hybridStoreImpl.get(id).left.value

                    value shouldBe a[JsonDecodingError]
                }
              }
            }
          }
        }
      }
    }
  }
}

trait HybridStoreWithOverwritesTestCases[
  IndexedStoreId,
  TypedStoreId,
  T,
  Namespace,
  TypedStoreImpl <: TypedStore[TypedStoreId, T],
  IndexedStoreImpl <: Store[IndexedStoreId, TypedStoreId],
  HybridStoreContext]
    extends HybridStoreTestCases[
      IndexedStoreId,
      TypedStoreId,
      T,
      Namespace,
      TypedStoreImpl,
      IndexedStoreImpl,
      HybridStoreContext]
    with StoreWithOverwritesTestCases[
      IndexedStoreId,
      T,
      Namespace,
      HybridStoreContext]

trait HybridStoreWithoutOverwritesTestCases[
  IndexedStoreId,
  TypedStoreId,
  T,
  Namespace,
  TypedStoreImpl <: TypedStore[TypedStoreId, T],
  IndexedStoreImpl <: Store[IndexedStoreId, TypedStoreId],
  HybridStoreContext]
    extends HybridStoreTestCases[
      IndexedStoreId,
      TypedStoreId,
      T,
      Namespace,
      TypedStoreImpl,
      IndexedStoreImpl,
      HybridStoreContext]
    with StoreWithoutOverwritesTestCases[
      IndexedStoreId,
      T,
      Namespace,
      HybridStoreContext]
