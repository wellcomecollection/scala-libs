package weco.storage.transfer.s3

import java.io.InputStream
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{CopyObjectRequest, ObjectTagging}
import com.amazonaws.services.s3.transfer.{Copy, TransferManager, TransferManagerBuilder}
import grizzled.slf4j.Logging
import org.apache.commons.io.IOUtils
import software.amazon.awssdk.services.s3.S3Client
import weco.storage.NotFoundError
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.store.s3.{S3StreamReadable, S3StreamStore}
import weco.storage.transfer._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class S3Transfer(transferManager: TransferManager, s3Readable: S3StreamReadable)
    extends Transfer[S3ObjectLocation, S3ObjectLocation]
    with Logging {

  import weco.storage.RetryOps._

  override def transfer(src: S3ObjectLocation,
                        dst: S3ObjectLocation): TransferEither =
    getStream(dst) match {

      // If the destination object doesn't exist, we can go ahead and
      // start the transfer.
      //
      // We have seen once case where the S3 CopyObject API returned
      // a 500 error, in a bag with multiple 20GB+ files, so we do need
      // to be able to retry failures here.
      case Left(_: NotFoundError) =>
        runTransfer(src, dst)

      case Left(e) =>
        warn(s"Unexpected error retrieving S3 object from $dst: $e")
        runTransfer(src, dst)

      case Right(dstStream) =>
        getStream(src) match {
          // If both the source and the destination exist, we can skip
          // the copy operation.
          case Right(srcStream) =>
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

  private def compare(src: S3ObjectLocation,
                      dst: S3ObjectLocation,
                      srcStream: InputStream,
                      dstStream: InputStream)
    : Either[TransferOverwriteFailure[S3ObjectLocation, S3ObjectLocation],
             TransferNoOp[S3ObjectLocation, S3ObjectLocation]] =
    if (IOUtils.contentEquals(srcStream, dstStream)) {
      Right(TransferNoOp(src, dst))
    } else {
      Left(TransferOverwriteFailure(src, dst))
    }

  private def getStream(location: S3ObjectLocation) =
    s3Readable.get(location).map(id => id.identifiedT)

  private def runTransfer(src: S3ObjectLocation,
                          dst: S3ObjectLocation): TransferEither =
    for {
      transfer <- tryCopyFromSource(src, dst)
        .retry(maxAttempts = 3)
        .left
        .map(err => TransferSourceFailure(src, dst, err.e))
      result <- tryCopyToDestination(src, dst, transfer)
        .retry(maxAttempts = 3)
        .left
        .map(err => TransferDestinationFailure(src, dst, err.e))
    } yield result

  private def tryCopyFromSource(src: S3ObjectLocation,
                                dst: S3ObjectLocation) = {
    // We use tags in the verifier in the storage service to check if we've already
    // verified an object.  For safety, we drop all the tags every time an object
    // gets rewritten or copied around.
    val copyRequest =
      new CopyObjectRequest(src.bucket, src.key, dst.bucket, dst.key)
        .withNewObjectTagging(new ObjectTagging(List().asJava))

    Try {
      // This code will throw if the source object doesn't exist.
      transferManager.copy(copyRequest)
    } match {
      case Success(request) => Right(request)
      case Failure(err) =>
        Left(S3Errors.readErrors(err))
    }
  }

  private def tryCopyToDestination(src: S3ObjectLocation,
                                   dst: S3ObjectLocation,
                                   transfer: Copy) =
    Try {
      transfer.waitForCopyResult()
    } match {
      case Success(_)   => Right(TransferPerformed(src, dst))
      case Failure(err) => Left(S3Errors.readErrors(err))
    }
}

object S3Transfer {
  def apply(implicit s3Client: AmazonS3, s3ClientV2: S3Client) = {
    val transferManager: TransferManager = TransferManagerBuilder.standard
      .withS3Client(s3Client)
      .build
    val s3Readable = new S3StreamStore()
    new S3Transfer(transferManager, s3Readable)
  }
}
