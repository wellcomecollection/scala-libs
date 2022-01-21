package weco.errors

/** Used to mark errors that can be retried.
  *
  * For example, a DynamoDB update() might fail with a ConditionalCheckFailed
  * exception if two processes try to write at the same time.  The operation
  * can be retried and will likely succeed.
  *
  */
trait RetryableError
