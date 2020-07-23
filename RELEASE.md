RELEASE_TYPE: major

*   The `TransferResult` type hierarchy has been rearranged:

    -   Results for a single Transfer and a PrefixTransfer are now two separate traits.
        This means that you don't have to deal with Transfer failures from the result of a PrefixTransfer, or vice versa.
    -   `PrefixTransferFailure` is now a base trait for all PrefixTransfer errors, and the previous case class has been renamed to `PrefixTransferIncomplete`.
        This result means that the prefix transfer was not completed successfully, but it may not have failed entirely.
    -   `PrefixTransferIncomplete` no longer holds an error (which was always empty).
    -   `PrefixTransferListingFailure` now includes the full ListingFailure, rather than the underlying exception.
