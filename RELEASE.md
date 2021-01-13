RELEASE_TYPE: patch

This release changes DynamoLockDao to use BatchWriteItem to release locks.  This will have no impact on callers, but should make unlocking faster and cheaper if you lock multiple IDs.