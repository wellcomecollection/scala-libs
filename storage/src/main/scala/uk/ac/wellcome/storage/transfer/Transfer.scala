package uk.ac.wellcome.storage.transfer

trait Transfer[SrcLocation, DstLocation] {
  type FailureResult = TransferFailure[SrcLocation, DstLocation]
  type SuccessResult = TransferSuccess[SrcLocation, DstLocation]

  type TransferEither = Either[FailureResult, SuccessResult]

  def transfer(src: SrcLocation, dst: DstLocation, checkForExisting: Boolean = true): TransferEither =
    if (checkForExisting) {
      transferWithCheckForExisting(src, dst)
    } else {
      transferWithOverwrites(src, dst)
    }

  protected def transferWithCheckForExisting(src: SrcLocation, dst: DstLocation): TransferEither
  protected def transferWithOverwrites(src: SrcLocation, dst: DstLocation): TransferEither
}
