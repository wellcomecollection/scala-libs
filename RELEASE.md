RELEASE_TYPE: minor

*   EnrichConfig now has helpers for getDurationOption and requireDuration
*   The Dynamo locking config now uses a `scala.concurrent.Duration` for the expiryTime rather than a `java.time.Duration`, and the expiry time is configurable in the typesafe builder with the `aws.dynamo.lockExpiryTime` config flag.
