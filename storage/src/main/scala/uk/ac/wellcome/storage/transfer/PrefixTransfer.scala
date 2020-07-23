package uk.ac.wellcome.storage.transfer

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.listing.Listing

import scala.collection.parallel.ParIterable

trait PrefixTransfer[Prefix, Location] extends Logging {
  implicit val transfer: Transfer[Location]
  implicit val listing: Listing[Prefix, Location]

  protected def buildDstLocation(
    srcPrefix: Prefix,
    dstPrefix: Prefix,
    srcLocation: Location
  ): Location

  private def copyPrefix(
    iterator: Iterable[Location],
    srcPrefix: Prefix,
    dstPrefix: Prefix,
    checkForExisting: Boolean
  ): Either[PrefixTransferIncomplete, PrefixTransferSuccess] = {
    var successes = 0
    var failures = 0

    iterator
      .grouped(10)
      .foreach { locations =>
        val results
          : ParIterable[(Location, Either[TransferFailure, TransferSuccess])] =
          locations.par.map { srcLocation =>
            (
              srcLocation,
              transfer.transfer(
                src = srcLocation,
                dst = buildDstLocation(
                  srcPrefix = srcPrefix,
                  dstPrefix = dstPrefix,
                  srcLocation = srcLocation
                ),
                checkForExisting = checkForExisting
              )
            )
          }

        results.foreach {
          case (srcLocation, Right(_)) =>
            debug(s"Successfully copied $srcLocation to $dstPrefix")
            successes += 1
          case (srcLocation, Left(err)) =>
            warn(s"Error copying $srcLocation to $dstPrefix: $err")
            failures += 1
        }
      }

    Either.cond(
      test = failures == 0,
      right = PrefixTransferSuccess(successes),
      left = PrefixTransferIncomplete(failures, successes)
    )
  }

  def transferPrefix(
    srcPrefix: Prefix,
    dstPrefix: Prefix,
    checkForExisting: Boolean = true
  ): Either[PrefixTransferFailure, PrefixTransferSuccess] = {
    listing.list(srcPrefix) match {
      case Left(error) =>
        Left(PrefixTransferListingFailure(srcPrefix, error))

      case Right(iterable) =>
        copyPrefix(
          iterator = iterable,
          srcPrefix = srcPrefix,
          dstPrefix = dstPrefix,
          checkForExisting = checkForExisting
        )
    }
  }
}
