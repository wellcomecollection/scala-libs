package uk.ac.wellcome.storage.transfer

sealed trait PrefixTransferResult

sealed trait PrefixTransferFailure extends PrefixTransferResult

case class PrefixTransferIncomplete(
  failures: Int,
  successes: Int,
  e: Throwable = new Error())
    extends PrefixTransferFailure

case class PrefixTransferListingFailure[Prefix](
  prefix: Prefix,
  e: Throwable = new Error())
    extends PrefixTransferFailure

case class PrefixTransferSuccess(successes: Int) extends PrefixTransferResult
