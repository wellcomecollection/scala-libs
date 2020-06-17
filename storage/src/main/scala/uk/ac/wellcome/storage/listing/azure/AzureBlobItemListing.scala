package uk.ac.wellcome.storage.listing.azure

import java.time.Duration

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.{BlobItem, ListBlobsOptions}
import uk.ac.wellcome.storage.{ListingFailure, ObjectLocationPrefix}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class AzureBlobItemListing(implicit blobClient: BlobServiceClient) extends AzureListing[BlobItem] {
  override def list(prefix: ObjectLocationPrefix): ListingResult =
    Try {
      val containerClient = blobClient.getBlobContainerClient(prefix.namespace)

      val options = new ListBlobsOptions().setPrefix(prefix.path)

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
