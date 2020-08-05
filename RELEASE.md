RELEASE_TYPE: patch

`send` on `MemoryIndividualMessageSender` now updates its internal message list using `synchronized` to prevent dropping entries with concurrent writes.
