RELEASE_TYPE: major

This changes the signature of `MessageSender.send` from `Try[Unit]` to `Either[MessageSenderError, Unit]`.

This means we can better distinguish errors when we send message notifications, e.g. we can mark a DNS issue when connecting to SNS as retryable.

Downstream users will need to adapt to the new function signature, and may want to add tests that they retry any errors that are marked as retryable.
