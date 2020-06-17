package uk.ac.wellcome.storage.tags.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{
  GetObjectTaggingRequest,
  ObjectTagging,
  SetObjectTaggingRequest,
  Tag
}
import uk.ac.wellcome.storage.s3.S3Errors
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags
import uk.ac.wellcome.storage.{
  ObjectLocation,
  ReadError,
  StoreWriteError,
  WriteError
}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class S3Tags(val maxRetries: Int = 3)(implicit s3Client: AmazonS3)
    extends Tags[ObjectLocation]
    with RetryableReadable[Map[String, String]] {

  override def retryableGetFunction(
    location: ObjectLocation): Map[String, String] = {
    val response = s3Client.getObjectTagging(
      new GetObjectTaggingRequest(location.namespace, location.path)
    )

    response.getTagSet.asScala.map { tag: Tag =>
      tag.getKey -> tag.getValue
    }.toMap
  }

  override def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)

  override protected def writeTags(
    location: ObjectLocation,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] = {
    val tagSet = tags
      .map { case (k, v) => new Tag(k, v) }
      .toSeq
      .asJava

    Try {
      s3Client.setObjectTagging(
        new SetObjectTaggingRequest(
          location.namespace,
          location.path,
          new ObjectTagging(tagSet)
        )
      )
    } match {
      case Success(_)   => Right(tags)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }
}
