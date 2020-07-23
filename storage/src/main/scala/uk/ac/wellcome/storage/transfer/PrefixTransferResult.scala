package uk.ac.wellcome.storage.transfer

import uk.ac.wellcome.storage.ListingFailure

sealed trait PrefixTransferResult

sealed trait PrefixTransferFailure extends PrefixTransferResult

case class PrefixTransferIncomplete(failures: Int, successes: Int)
    extends PrefixTransferFailure

case class PrefixTransferListingFailure[Prefix](prefix: Prefix,
                                                e: ListingFailure[Prefix])
    extends PrefixTransferFailure

case class PrefixTransferSuccess(successes: Int) extends PrefixTransferResult
