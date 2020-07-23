package uk.ac.wellcome.storage.tags.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{
  GetObjectTaggingRequest,
  ObjectTagging,
  SetObjectTaggingRequest,
  Tag
}
import uk.ac.wellcome.storage.s3.{S3Errors, S3ObjectLocation}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags
import uk.ac.wellcome.storage.{ReadError, StoreWriteError, WriteError}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class S3Tags(val maxRetries: Int = 3)(implicit s3Client: AmazonS3)
    extends Tags[S3ObjectLocation]
    with RetryableReadable[S3ObjectLocation, Map[String, String]] {

  override def retryableGetFunction(
    location: S3ObjectLocation): Map[String, String] = {
    val response = s3Client.getObjectTagging(
      new GetObjectTaggingRequest(location.bucket, location.key)
    )

    response.getTagSet.asScala.map { tag: Tag =>
      tag.getKey -> tag.getValue
    }.toMap
  }

  override def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)

  override protected def writeTags(
    location: S3ObjectLocation,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] = {
    val tagSet = tags
      .map { case (k, v) => new Tag(k, v) }
      .toSeq
      .asJava

    Try {
      s3Client.setObjectTagging(
        new SetObjectTaggingRequest(
          location.bucket,
          location.key,
          new ObjectTagging(tagSet)
        )
      )
    } match {
      case Success(_)   => Right(tags)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }
}
