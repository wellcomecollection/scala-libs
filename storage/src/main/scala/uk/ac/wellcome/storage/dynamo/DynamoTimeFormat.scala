package uk.ac.wellcome.storage.dynamo

import java.time.Instant

import org.scanamo.DynamoFormat

object DynamoTimeFormat {

  // We use this rather than scanamo.time.JavaTimeFormats._ because Dynamo's
  // builtin item expiry feature requires the TTL value to be in seconds,
  // rather than the library's milliseconds since the epoch.
  //
  // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-before-you-start.html#time-to-live-ttl-before-you-start-formatting
  // https://github.com/scanamo/scanamo/blob/v1.0.0-M10/java-time/src/main/scala/org/scanamo/time/JavaTimeFormats.scala#L20
  implicit val instantAsLongSecondsFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, ArithmeticException](
      Instant.ofEpochSecond,
      _.getEpochSecond
    )
}
