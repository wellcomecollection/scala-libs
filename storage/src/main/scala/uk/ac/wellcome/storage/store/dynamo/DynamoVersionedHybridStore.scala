package uk.ac.wellcome.storage.store.dynamo

import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.VersionedHybridStore

class DynamoVersionedHybridStore[Id, V, T](
  store: DynamoHybridStoreWithMaxima[Id, V, T])(implicit N: Numeric[V])
    extends VersionedHybridStore[Id, V, S3ObjectLocation, T](store)
