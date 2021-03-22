RELEASE_TYPE: patch

SQSStream and AlpakkaSQSWorker are now using DeleteMessageBatch rather than DeleteMessage.
This should have no user-visible effects.