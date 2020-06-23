package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.StreamStore

class AzureStreamStore(
  val maxRetries: Int = 2,
  val allowOverwrites: Boolean = true
)(implicit val blobClient: BlobServiceClient)
    extends StreamStore[ObjectLocation]
    with AzureStreamReadable
    with AzureStreamWritable
