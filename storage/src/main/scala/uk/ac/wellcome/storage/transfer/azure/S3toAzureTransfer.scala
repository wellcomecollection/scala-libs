package uk.ac.wellcome.storage.transfer.azure

import java.io.InputStream

import com.azure.storage.blob.BlobServiceClient
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.azure.{AzureStreamReadable, AzureStreamStore, AzureStreamWritable}
import uk.ac.wellcome.storage.store.s3.S3StreamReadable
import uk.ac.wellcome.storage.transfer._
import uk.ac.wellcome.storage.{DoesNotExistError, Identified}

class S3toAzureTransfer(implicit
                        s3Readable: S3StreamReadable,
                        blobClient: BlobServiceClient)
    extends Transfer[S3ObjectLocation, AzureBlobLocation] {
  import uk.ac.wellcome.storage.RetryOps._

  private val azureStreamStore: AzureStreamStore = new AzureStreamStore(
    allowOverwrites = true)

  private val azureWritable: AzureStreamWritable = azureStreamStore
  private val azureReadable: AzureStreamReadable = azureStreamStore

  override protected def transferWithCheckForExisting(
    src: S3ObjectLocation,
    dst: AzureBlobLocation): TransferEither =
    azureReadable.get(dst) match {

      // If the destination object doesn't exist, we can go ahead and start the
      // transfer.
      case Left(_: DoesNotExistError) =>
        transferWithOverwrites(src, dst)

      // Any other sort of error needs further investigation; don't try to write
      // over a blob that's misbehaving.
      case Left(err) =>
        Left(TransferDestinationFailure(src, dst, err.e))

      case Right(Identified(_, dstStream)) =>
        s3Readable.get(src.toObjectLocation) match {
          case Right(Identified(_, srcStream)) =>
            val result = compare(
              src = src,
              dst = dst,
              srcStream = srcStream,
              dstStream = dstStream
            )

            // Remember to close the streams afterwards, or we might get
            // errors from the S3 SDK like:
            //
            //    Unable to execute HTTP request: Timeout waiting for
            //    connection from pool
            //
            // See: https://github.com/wellcometrust/platform/issues/3600
            //      https://github.com/aws/aws-sdk-java/issues/269
            //
            srcStream.close()
            dstStream.close()

            result

          case Left(err) =>
            // As above, we need to abort the input stream so we don't leave streams
            // open or get warnings from the SDK.
            dstStream.close()
            Left(TransferSourceFailure(src, dst, err.e))
        }
    }

  override protected def transferWithOverwrites(
    src: S3ObjectLocation,
    dst: AzureBlobLocation): TransferEither = {
    def singleTransfer: TransferEither =
      runTransfer(src, dst)

    singleTransfer.retry(maxAttempts = 3)
  }

  private def compare(src: S3ObjectLocation,
                      dst: AzureBlobLocation,
                      srcStream: InputStream,
                      dstStream: InputStream)
    : Either[TransferOverwriteFailure[S3ObjectLocation, AzureBlobLocation],
             TransferNoOp[S3ObjectLocation, AzureBlobLocation]] =
    if (IOUtils.contentEquals(srcStream, dstStream)) {
      Right(TransferNoOp(src, dst))
    } else {
      Left(TransferOverwriteFailure(src, dst))
    }

  private def runTransfer(src: S3ObjectLocation,
                          dst: AzureBlobLocation): TransferEither =
    s3Readable.get(src.toObjectLocation) match {
      case Right(Identified(_, srcStream)) =>
        azureWritable.put(dst)(srcStream) match {
          case Right(_)  => Right(TransferPerformed(src, dst))
          case Left(err) => Left(TransferDestinationFailure(src, dst, err.e))
        }

      case Left(err) => Left(TransferSourceFailure(src, dst, err.e))
    }
}
