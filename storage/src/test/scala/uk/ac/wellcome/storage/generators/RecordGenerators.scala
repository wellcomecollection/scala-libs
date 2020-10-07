package uk.ac.wellcome.storage.generators

import grizzled.slf4j.Logging
import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.IdentityKey
import uk.ac.wellcome.storage.streaming.Codec
import uk.ac.wellcome.storage.streaming.Codec._

case class Record(name: String)

trait RecordGenerators extends RandomGenerators with Logging {
  implicit lazy val codec: Codec[Record] = typeCodec[Record]

  def createIdentityKey: IdentityKey =
    IdentityKey(randomAlphanumeric())

  def createRecord: Record = {
    val record = Record(name = randomAlphanumeric())

    trace(s"Created Record: $record")

    record
  }

  def createRecords: Set[Record] =
    collectionOf(min = 100, max = 200) { createRecord }.toSet
}
