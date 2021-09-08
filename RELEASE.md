RELEASE_TYPE: patch

All instances of StorageError now include a custom exception message.
This should be easier to debug than seeing `java.lang.Error: null` in the logs.