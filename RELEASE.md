RELEASE_TYPE: patch

VersionedStore.put is now idempotent.
That is, if you call `put(id, t)` with the same values repeatedly, it will succeed – previously it would through VersionAlreadyExistsError or HigherVersionExists on subsequent calls.