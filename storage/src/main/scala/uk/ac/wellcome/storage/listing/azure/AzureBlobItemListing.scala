package uk.ac.wellcome.storage.listing.azure

import java.time.Duration

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.{BlobItem, ListBlobsOptions}
import uk.ac.wellcome.storage.ListingFailure
import uk.ac.wellcome.storage.azure.AzureBlobLocationPrefix

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class AzureBlobItemListing(implicit blobClient: BlobServiceClient)
    extends AzureListing[BlobItem] {
  override def list(prefix: AzureBlobLocationPrefix): ListingResult =
    Try {
      val containerClient = blobClient.getBlobContainerClient(prefix.container)

      val options = new ListBlobsOptions().setPrefix(prefix.namePrefix)

      val items: Iterable[BlobItem] = containerClient
        .listBlobs(options, Duration.ofSeconds(5))
        .iterator()
        .asScala
        .toIterable

      items
    } match {
      case Failure(err)   => Left(ListingFailure(prefix, err))
      case Success(items) => Right(items)
    }
}
