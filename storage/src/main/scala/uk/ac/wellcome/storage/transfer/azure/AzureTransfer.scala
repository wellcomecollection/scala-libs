package uk.ac.wellcome.storage.transfer.azure

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL

import software.amazon.awssdk.services.s3.AmazonS3
import software.amazon.awssdk.services.s3.model.{S3ObjectInputStream, S3ObjectSummary}
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.{
  BlobRange,
  BlobStorageException,
  BlockListType
}
import com.azure.storage.blob.specialized.{BlobInputStream, BlockBlobClient}
import grizzled.slf4j.Logging
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.models.ByteRange
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.services.azure.AzureSizeFinder
import uk.ac.wellcome.storage.services.s3.{S3RangedReader, S3Uploader}
import uk.ac.wellcome.storage.transfer._
import uk.ac.wellcome.storage.{RetryableError, StoreWriteError}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class AzureSourceTransferException(msg: String) extends RuntimeException(msg)

trait AzureTransfer[Context]
    extends Transfer[S3ObjectSummary, AzureBlobLocation]
    with Logging {
  implicit val s3Client: AmazonS3
  implicit val blobServiceClient: BlobServiceClient

  import uk.ac.wellcome.storage.RetryOps._

  val blockSize: Long

  private def runTransfer(
    src: S3ObjectSummary,
    dst: AzureBlobLocation,
    allowOverwrites: Boolean
  ): Either[TransferFailure[S3ObjectSummary, AzureBlobLocation], Unit] = {
    for {

      context <- getContext(src, dst)

      result <- writeBlocks(
        src = S3ObjectLocation(src),
        dst = dst,
        s3Length = src.getSize,
        allowOverwrites = allowOverwrites,
        context = context
      ) match {
        case Success(_) => Right(())
        case Failure(err: AzureSourceTransferException) =>
          Left(TransferSourceFailure(src, dst, err))
        case Failure(err) => Left(TransferDestinationFailure(src, dst, err))
      }
    } yield result
  }

  protected def getContext(
    src: S3ObjectSummary,
    dst: AzureBlobLocation
  ): Either[TransferSourceFailure[S3ObjectSummary, AzureBlobLocation], Context]

  protected def writeBlockToAzure(
    src: S3ObjectLocation,
    dst: AzureBlobLocation,
    range: BlobRange,
    blockId: String,
    s3Length: Long,
    context: Context
  ): Unit

  private def writeBlocks(
    src: S3ObjectLocation,
    dst: AzureBlobLocation,
    s3Length: Long,
    allowOverwrites: Boolean,
    context: Context
  ): Try[Unit] = {
    val blockClient = blobServiceClient
      .getBlobContainerClient(dst.container)
      .getBlobClient(dst.name)
      .getBlockBlobClient

    // To write a large blob to Azure, we write a series of individual blocks
    // Each block has a deterministic ID -- 1, 2, 3, 4, and so on, which then gets
    // zero-padded to a fixed length and base64 encoded.
    //
    // When we've written every block, we "commit" them.  This causes Azure to
    // stitch them together into a single block.
    val ranges =
      BlobRangeUtil.getRanges(length = s3Length, blockSize = blockSize)
    val identifiers = BlobRangeUtil.getBlockIdentifiers(count = ranges.size)

    // We pay to transfer data out of AWS; look up any blocks that we've already
    // written (e.g. by an interrupted replicator) and skip rewriting them.
    // This makes the transfer more reliable and saves money!
    //
    // If they do somehow contain corrupt data, it'll be caught by the verifier.
    val uncommittedBlockIds: Set[String] = getUncommittedBlockIds(blockClient)

    Try {
      identifiers.zip(ranges).foreach {
        case (blockId, range) =>
          if (uncommittedBlockIds.contains(blockId)) {
            debug(
              s"Skipping upload to $dst with range $range / block ID $blockId (this block already exists)"
            )
          } else {
            debug(s"Uploading to $dst with range $range / block ID $blockId")

            // For very large objects, we have to successfully Put a lot of blocks for
            // the transfer to succeed.
            //
            // Retry the Put of each individual block three times before we give up.
            def writeOnce: (
              (
                S3ObjectLocation,
                AzureBlobLocation,
                BlobRange,
                String,
                Long,
                Context
              )
            ) => Either[StoreWriteError with RetryableError, Unit] = {
              args: (
                S3ObjectLocation,
                AzureBlobLocation,
                BlobRange,
                String,
                Long,
                Context
              ) =>
                val (src, dst, range, blockId, s3Length, context) = args

                Try {
                  writeBlockToAzure(
                    src = src,
                    dst = dst,
                    range = range,
                    blockId = blockId,
                    s3Length = s3Length,
                    context = context
                  )
                } match {
                  case Success(_) => Right(())
                  case Failure(err) =>
                    warn(
                      s"Error while trying to Put Block to $dst range $range: $err"
                    )
                    Left(new StoreWriteError(err) with RetryableError)
                }
            }

            writeOnce.retry(maxAttempts = 3)(
              (src, dst, range, blockId, s3Length, context)
            ) match {
              case Right(_)  => ()
              case Left(err) => throw err.e
            }
          }
      }

      blockClient.commitBlockList(identifiers.toList.asJava, allowOverwrites)
    }
  }

  private def getUncommittedBlockIds(
    blockClient: BlockBlobClient
  ): Set[String] =
    Try {
      blockClient
        .listBlocks(BlockListType.UNCOMMITTED)
        .getUncommittedBlocks
        .asScala
        .map { _.getName }
        .toSet
    } match {
      case Success(blockIds) => blockIds
      // What if we're the first person to write to this blob?
      case Failure(exc: BlobStorageException) if exc.getStatusCode == 404 =>
        Set.empty
      case Failure(err) => throw err
    }

  override protected def transferWithCheckForExisting(
    src: S3ObjectSummary,
    dst: AzureBlobLocation
  ): TransferEither =
    getAzureStream(dst) match {
      // If the destination object doesn't exist, we can go ahead and
      // start the transfer.
      case Failure(_) =>
        transferWithOverwrites(src, dst)

      case Success(dstStream) =>
        val srcObjectLocation = S3ObjectLocation(src)
        getS3Stream(srcObjectLocation) match {
          // If both the source and the destination exist, we can skip
          // the copy operation.
          case Success(srcStream) =>
            val result = compare(
              src = src,
              dst = dst,
              srcStream = srcStream,
              dstStream = dstStream
            )

            // Remember to close the streams afterwards, or we might get
            // errors like
            //
            //    Unable to execute HTTP request: Timeout waiting for
            //    connection from pool
            //
            // See: https://github.com/wellcometrust/platform/issues/3600
            //      https://github.com/aws/aws-sdk-java/issues/269
            //
            srcStream.abort()
            srcStream.close()
            dstStream.close()

            result

          case Failure(err) =>
            // As above, we need to abort the input stream so we don't leave streams
            // open or get warnings from the SDK.
            dstStream.close()
            Left(TransferSourceFailure(src, dst, err))
        }
    }

  private def getS3Stream(
    location: S3ObjectLocation
  ): Try[S3ObjectInputStream] =
    Try {
      s3Client.getObject(location.bucket, location.key)
    }.map { _.getObjectContent }

  private def getAzureStream(
    location: AzureBlobLocation
  ): Try[BlobInputStream] =
    Try {
      blobServiceClient
        .getBlobContainerClient(location.container)
        .getBlobClient(location.name)
        .openInputStream()
    }

  protected def compare(
    src: S3ObjectSummary,
    dst: AzureBlobLocation,
    srcStream: InputStream,
    dstStream: InputStream
  ): Either[
    TransferOverwriteFailure[S3ObjectSummary, AzureBlobLocation],
    TransferNoOp[S3ObjectSummary, AzureBlobLocation]
  ] =
    if (IOUtils.contentEquals(srcStream, dstStream)) {
      Right(TransferNoOp(src, dst))
    } else {
      Left(TransferOverwriteFailure(src, dst))
    }

  override protected def transferWithOverwrites(
    src: S3ObjectSummary,
    dst: AzureBlobLocation
  ): TransferEither =
    runTransfer(src, dst, allowOverwrites = true).map { _ =>
      TransferPerformed(src, dst)
    }
}

class AzurePutBlockTransfer(
  // The max length you can put in a single Put Block API call is 4000 MiB.
  // The class will load a block of this size into memory, so setting it too
  // high may cause issues.
  val blockSize: Long = 1000000000
)(
  implicit
  val s3Client: AmazonS3,
  val blobServiceClient: BlobServiceClient
) extends AzureTransfer[Unit] {

  private val rangedReader = new S3RangedReader()

  override protected def getContext(
    src: S3ObjectSummary,
    dst: AzureBlobLocation
  ): Either[TransferSourceFailure[S3ObjectSummary, AzureBlobLocation], Unit] =
    Right(())

  override protected def writeBlockToAzure(
    src: S3ObjectLocation,
    dst: AzureBlobLocation,
    range: BlobRange,
    blockId: String,
    s3Length: Long,
    context: Unit
  ): Unit = {
    // Note: this class is only used in the tests as a way to exercise the
    // logic in AzureTransfer[…], so the raw exception here is okay.
    val bytes = rangedReader.getBytes(
      location = src,
      range = ByteRange(range)
    ) match {
      case Right(value) => value
      case Left(err) =>
        throw new AzureSourceTransferException(
          s"Error reading chunk from S3 location $src (range $range): $err"
        )
    }

    val blockClient = blobServiceClient
      .getBlobContainerClient(dst.container)
      .getBlobClient(dst.name)
      .getBlockBlobClient

    blockClient.stageBlock(
      blockId,
      new ByteArrayInputStream(bytes),
      bytes.length
    )
  }
}

class AzurePutBlockFromUrlTransfer(s3Uploader: S3Uploader,
                                   azureSizeFinder: AzureSizeFinder,
                                   blockTransfer: AzurePutBlockTransfer)(
  signedUrlValidity: FiniteDuration,
  val blockSize: Long)(
  implicit
  val s3Client: AmazonS3,
  val blobServiceClient: BlobServiceClient
) extends AzureTransfer[URL] {

  override protected def getContext(
    src: S3ObjectSummary,
    dst: AzureBlobLocation
  ): Either[TransferSourceFailure[S3ObjectSummary, AzureBlobLocation], URL] =
    s3Uploader
      .getPresignedGetURL(
        S3ObjectLocation(src),
        expiryLength = signedUrlValidity)
      .left
      .map { readError =>
        TransferSourceFailure(src, dst, e = readError.e)
      }

  override protected def writeBlockToAzure(
    src: S3ObjectLocation,
    dst: AzureBlobLocation,
    range: BlobRange,
    blockId: String,
    s3Length: Long,
    presignedUrl: URL
  ): Unit = {
    val blockClient = blobServiceClient
      .getBlobContainerClient(dst.container)
      .getBlobClient(dst.name)
      .getBlockBlobClient

    blockClient.stageBlockFromUrl(blockId, presignedUrl.toString, range)
  }

  override protected def compare(
    src: S3ObjectSummary,
    dst: AzureBlobLocation,
    srcStream: InputStream,
    dstStream: InputStream
  ): Either[
    TransferOverwriteFailure[S3ObjectSummary, AzureBlobLocation],
    TransferNoOp[S3ObjectSummary, AzureBlobLocation]
  ] =
    (src.getSize, azureSizeFinder.getSize(dst)) match {
      case (srcSize, Right(dstSize)) if srcSize == dstSize =>
        Right(TransferNoOp(src, dst))

      case _ =>
        Left(
          TransferOverwriteFailure(
            src,
            dst,
            e = new Throwable(s"Sizes of $src and $dst don't match!")
          )
        )
    }

  private def isWeirdKey(key: String): Boolean =
    key.endsWith(".")

  override def transfer(
    src: S3ObjectSummary,
    dst: AzureBlobLocation,
    checkForExisting: Boolean
  ): TransferEither =
    if (isWeirdKey(src.getKey)) {
      blockTransfer.transfer(src, dst, checkForExisting)
    } else {
      super.transfer(src, dst, checkForExisting)
    }
}

object AzurePutBlockFromUrlTransfer {
  def apply(signedUrlValidity: FiniteDuration, blockSize: Long)(
    implicit s3Client: AmazonS3,
    blobServiceClient: BlobServiceClient) = {
    val s3Uploader = new S3Uploader()

    // In the actual replicator, if there's an object in S3 and an object in Azure,
    // assume they're both the same.  The verifier will validate the checksum later.
    //
    // This means that if the replicator is interrupted, it won't wait (and cost money)
    // to read the existing blob out of Azure.  If the sizes match, it's good enough
    // for the replicator to assume they're the same.
    val azureSizeFinder = new AzureSizeFinder()

    // Using PutBlockFromURL requires us to create a pre-signed URL for GETing
    // the object from S3, but some S3 keys are a bit tricky to use in URLs,
    // and when Azure tries to read them, it gets an error:
    //
    //      com.azure.storage.blob.models.BlobStorageException: Status code 403,
    //      <?xml version="1.0" encoding="utf-8"?><Error><Code>CannotVerifyCopySource…
    //
    // In those cases, fall back to transferring the bytes directly through
    // the replicator.  We try to avoid this because it's slower and puts more
    // memory pressure on the replicator, but is acceptable if we're only doing
    // it for a handful of keys.
    val blockTransfer = new AzurePutBlockTransfer(blockSize = blockSize)
    new AzurePutBlockFromUrlTransfer(
      s3Uploader,
      azureSizeFinder,
      blockTransfer)(signedUrlValidity, blockSize)
  }
}
