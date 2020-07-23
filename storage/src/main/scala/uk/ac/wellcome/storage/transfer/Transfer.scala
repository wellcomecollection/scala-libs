package uk.ac.wellcome.storage.transfer

trait Transfer[Location] {
  type FailureResult = TransferFailure[Location, Location]
  type SuccessResult = TransferSuccess[Location, Location]

  type TransferEither = Either[FailureResult, SuccessResult]

  def transfer(src: Location, dst: Location, checkForExisting: Boolean = true): TransferEither =
    if (checkForExisting) {
      transferWithCheckForExisting(src, dst)
    } else {
      transferWithOverwrites(src, dst)
    }

  protected def transferWithCheckForExisting(src: Location, dst: Location): TransferEither
  protected def transferWithOverwrites(src: Location, dst: Location): TransferEither
}
