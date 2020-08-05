package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.store.TypedStore
import uk.ac.wellcome.storage.streaming.Codec

class AzureTypedStore[T](
  implicit val codec: Codec[T],
  val streamStore: AzureStreamStore
) extends TypedStore[AzureBlobLocation, T]

object AzureTypedStore {
  def apply[T](implicit codec: Codec[T],
               blobServiceClient: BlobServiceClient): AzureTypedStore[T] = {
    implicit val streamStore: AzureStreamStore = new AzureStreamStore()

    new AzureTypedStore[T]()
  }
}
