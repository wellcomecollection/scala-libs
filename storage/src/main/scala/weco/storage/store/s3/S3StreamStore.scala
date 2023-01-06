package weco.storage.store.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.transfer.s3.S3TransferManager
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.StreamStore

class S3StreamStore(val maxRetries: Int = 2)(
  implicit val s3Client: S3Client,
  val transferManager: S3TransferManager)
    extends StreamStore[S3ObjectLocation]
    with S3StreamReadable
    with S3StreamWritable
