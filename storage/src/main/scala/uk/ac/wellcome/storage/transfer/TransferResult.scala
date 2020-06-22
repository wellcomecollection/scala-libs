package uk.ac.wellcome.storage.transfer

sealed trait TransferResult

sealed trait TransferFailure extends TransferResult {
  val e: Throwable
}

case class TransferSourceFailure[SrcLocation, DstLocation](
  source: SrcLocation,
  destination: DstLocation,
  e: Throwable = new Error())
    extends TransferFailure

case class TransferDestinationFailure[SrcLocation, DstLocation](
  source: SrcLocation,
  destination: DstLocation,
  e: Throwable = new Error())
    extends TransferFailure

case class TransferOverwriteFailure[SrcLocation, DstLocation](
  source: SrcLocation,
  destination: DstLocation,
  e: Throwable = new Error())
    extends TransferFailure

case class PrefixTransferFailure(failures: Int,
                                 successes: Int,
                                 e: Throwable = new Error())
    extends TransferFailure

case class PrefixTransferListingFailure[Prefix](prefix: Prefix,
                                                e: Throwable = new Error())
    extends TransferFailure

sealed trait TransferSuccess extends TransferResult

case class TransferNoOp[SrcLocation, DstLocation](source: SrcLocation,
                                                  destination: DstLocation)
    extends TransferSuccess

case class TransferPerformed[SrcLocation, DstLocation](source: SrcLocation,
                                                       destination: DstLocation)
    extends TransferSuccess

case class PrefixTransferSuccess(successes: Int) extends TransferSuccess
