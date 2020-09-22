package uk.ac.wellcome.storage.services.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.services.{
  LargeStreamReader,
  RangedReader,
  SizeFinder
}

class AzureLargeStreamReader(
  val bufferSize: Long,
  val sizeFinder: SizeFinder[AzureBlobLocation],
  val rangedReader: RangedReader[AzureBlobLocation]
) extends LargeStreamReader[AzureBlobLocation]

object AzureLargeStreamReader {
  def apply(bufferSize: Long)(implicit blobServiceClient: BlobServiceClient) = {
    val sizeFinder: SizeFinder[AzureBlobLocation] = new AzureSizeFinder()

    val rangedReader: RangedReader[AzureBlobLocation] = new AzureRangedReader()
    new AzureLargeStreamReader(bufferSize, sizeFinder, rangedReader)

  }
}
