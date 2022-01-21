RELEASE_TYPE: major

This release adds better retrying when there are network connectivity issues with SNS and S3, in particular:

-   when we can resolve the endpoint, but can't connect
-   when we can't resolve the endpoint

Both of these errors will be instances of `RetryableError`, and should be retried appropriately.

As a side effect, the signature of `MessageSender.send` has changed from `Try[Unit]` to `Either[MessageSender, Unit]`, so we can better distinguish errors when we send message notifications.
Downstream users will need to adapt to the new function signature, and may want to add tests that they retry any errors that are marked as retryable.
