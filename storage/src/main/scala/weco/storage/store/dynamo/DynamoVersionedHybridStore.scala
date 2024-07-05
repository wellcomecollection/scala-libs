package weco.storage.store.dynamo

import weco.storage.providers.s3.S3ObjectLocation
import weco.storage.store.VersionedHybridStore

class DynamoVersionedHybridStore[Id, V, T](
  store: DynamoHybridStoreWithMaxima[Id, V, T])(implicit N: Numeric[V])
    extends VersionedHybridStore[Id, V, S3ObjectLocation, T](store)
