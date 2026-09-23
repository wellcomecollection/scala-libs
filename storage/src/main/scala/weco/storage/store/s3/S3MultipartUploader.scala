package weco.storage.store.s3

import grizzled.slf4j.Logging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  ChecksumAlgorithm,
  CompleteMultipartUploadRequest,
  CompleteMultipartUploadResponse,
  CompletedMultipartUpload,
  CompletedPart,
  CreateMultipartUploadRequest,
  UploadPartRequest
}
import weco.storage.providers.s3.S3ObjectLocation

import scala.collection.JavaConverters._
import scala.util.Try

trait S3MultipartUploader extends Logging {
  implicit val s3Client: S3Client

  /** Starts a multipart upload and returns the multipart upload ID */
  def createMultipartUpload(location: S3ObjectLocation): Try[String] =
    Try {
      val createRequest =
        CreateMultipartUploadRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          // Newer SDKs checksum every part by default, so declare the algorithm up front
          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
          .build()

      val createResponse = s3Client.createMultipartUpload(createRequest)

      debug(
        s"Got CreateMultipartUploadResponse to $location with upload ID ${createResponse.uploadId()}"
      )

      createResponse.uploadId()
    }

  def uploadPart(location: S3ObjectLocation,
                 uploadId: String,
                 bytes: Array[Byte],
                 partNumber: Int): Try[CompletedPart] =
    Try {
      val uploadPartRequest =
        UploadPartRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          .uploadId(uploadId)
          .partNumber(partNumber)
          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
          .build()

      val requestBody = RequestBody.fromBytes(bytes)

      val uploadPartResponse =
        s3Client.uploadPart(uploadPartRequest, requestBody)

      CompletedPart
        .builder()
        .eTag(uploadPartResponse.eTag())
        // S3 wants each part's checksum back on completion
        .checksumCRC32(uploadPartResponse.checksumCRC32())
        .partNumber(partNumber)
        .build()
    }

  def completeMultipartUpload(
    location: S3ObjectLocation,
    uploadId: String,
    completedParts: List[CompletedPart]): Try[CompleteMultipartUploadResponse] =
    Try {
      val completedMultipartUpload =
        CompletedMultipartUpload
          .builder()
          .parts(completedParts.asJava)
          .build()

      val completeRequest =
        CompleteMultipartUploadRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          .uploadId(uploadId)
          .multipartUpload(completedMultipartUpload)
          .build()

      s3Client.completeMultipartUpload(completeRequest)
    }
}
