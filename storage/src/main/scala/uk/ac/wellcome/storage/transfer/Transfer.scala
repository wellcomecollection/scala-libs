package uk.ac.wellcome.storage.transfer

trait Transfer[SrcLocation, DstLocation] {
  def transfer(src: SrcLocation,
               dst: DstLocation,
               checkForExisting: Boolean = true)
    : Either[TransferFailure, TransferSuccess] =
    if (checkForExisting) {
      transferWithCheckForExisting(src, dst)
    } else {
      transferWithOverwrites(src, dst)
    }

  protected def transferWithCheckForExisting(
    src: SrcLocation,
    dst: DstLocation): Either[TransferFailure, TransferSuccess]

  protected def transferWithOverwrites(
    src: SrcLocation,
    dst: DstLocation): Either[TransferFailure, TransferSuccess]
}
