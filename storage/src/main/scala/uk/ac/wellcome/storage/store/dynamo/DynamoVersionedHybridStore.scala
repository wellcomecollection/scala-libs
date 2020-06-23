package uk.ac.wellcome.storage.store.dynamo

import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.VersionedHybridStore

class DynamoVersionedHybridStore[Id, V, T](
  store: DynamoHybridStoreWithMaxima[Id, V, T])(implicit N: Numeric[V])
    extends VersionedHybridStore[Id, V, ObjectLocation, T](store)
