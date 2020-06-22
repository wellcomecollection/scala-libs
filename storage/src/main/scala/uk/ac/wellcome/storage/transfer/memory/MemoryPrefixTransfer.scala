package uk.ac.wellcome.storage.transfer.memory

import uk.ac.wellcome.storage.{Location, Prefix}
import uk.ac.wellcome.storage.listing.memory.MemoryListing
import uk.ac.wellcome.storage.transfer.PrefixTransfer

trait MemoryPrefixTransfer[Ident <: Location, MemPrefix <: Prefix[Ident], T]
    extends PrefixTransfer[Ident, MemPrefix, Ident, MemPrefix]
    with MemoryTransfer[Ident, T]
    with MemoryListing[Ident, MemPrefix, T] {
  implicit val transfer: MemoryTransfer[Ident, T] = this
  implicit val listing: MemoryListing[Ident, MemPrefix, T] = this
}
