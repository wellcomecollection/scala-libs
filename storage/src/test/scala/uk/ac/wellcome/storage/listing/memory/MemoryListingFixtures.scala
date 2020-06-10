package uk.ac.wellcome.storage.listing.memory

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.listing.fixtures.ListingFixtures
import uk.ac.wellcome.storage.store.memory.MemoryStore

trait MemoryListingFixtures[T]
    extends ListingFixtures[
      String,
      String,
      String,
      MemoryListing[String, String, T],
      MemoryStore[String, T]] {
  def createT: T

  override def withListingContext[R](
    testWith: TestWith[MemoryStore[String, T], R]): R =
    testWith(
      new MemoryStore[String, T](
        initialEntries = Map.empty
      )
    )

  override def withListing[R](context: MemoryStore[String, T],
                              initialEntries: Seq[String])(
    testWith: TestWith[MemoryListing[String, String, T], R]): R =
    testWith(
      new MemoryListing[String, String, T] {
        override var entries: Map[String, T] = initialEntries.map { id =>
          (id, createT)
        }.toMap

        override protected def startsWith(id: String,
                                          prefix: String): Boolean =
          id.startsWith(prefix)
      }
    )
}
