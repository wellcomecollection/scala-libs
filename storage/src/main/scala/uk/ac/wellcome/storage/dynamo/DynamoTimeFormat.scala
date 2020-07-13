package uk.ac.wellcome.storage.dynamo

import java.time.Instant

import org.scanamo.DynamoFormat

object DynamoTimeFormat {
  implicit val instantAsLongSecondsFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, ArithmeticException](x =>
      Instant.ofEpochSecond(x))(x => x.getEpochSecond)
}
