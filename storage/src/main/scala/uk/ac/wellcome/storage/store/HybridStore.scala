package uk.ac.wellcome.storage.store

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage._

trait HybridStore[IndexedStoreId, TypedStoreId, T]
    extends Store[IndexedStoreId, T]
    with Logging {

  implicit protected val indexedStore: Store[IndexedStoreId, TypedStoreId]
  implicit protected val typedStore: TypedStore[TypedStoreId, T]

  protected def createTypeStoreId(id: IndexedStoreId): TypedStoreId

  override def get(id: IndexedStoreId): ReadEither =
    for {
      typedStoreId <- indexedStore.get(id)
      item <- getTypedStoreEntry(typedStoreId.identifiedT)
    } yield Identified(id, item.identifiedT)

  // If the indexed store points to a typed store entry that doesn't exist, that
  // suggests an internal error in the store, so we don't want to bubble up
  // the DoesNotExistError directly.
  protected def getTypedStoreEntry(typedStoreId: TypedStoreId)
    : Either[ReadError, Identified[TypedStoreId, T]] =
    typedStore.get(typedStoreId) match {
      case Right(t) => Right(t)
      case Left(err: DoesNotExistError) =>
        Left(DanglingHybridStorePointerError(err.e))
      case Left(err) => Left(err)
    }

  override def put(id: IndexedStoreId)(item: T): WriteEither = {
    val typeStoreId = createTypeStoreId(id)

    for {
      item <- typedStore.put(typeStoreId)(item).map(_.identifiedT)
      id <- indexedStore.put(id)(typeStoreId) match {
        case Left(error)              => Left(StoreWriteError(error.e))
        case Right(Identified(id, _)) => Right(id)
      }

    } yield Identified(id, item)
  }
}
