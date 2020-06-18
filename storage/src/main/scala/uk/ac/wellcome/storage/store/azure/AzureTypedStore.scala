package uk.ac.wellcome.storage.store.azure

import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.TypedStore
import uk.ac.wellcome.storage.streaming.Codec

class AzureTypedStore[T](
  implicit val codec: Codec[T],
  val streamStore: AzureStreamStore
) extends TypedStore[ObjectLocation, T]

