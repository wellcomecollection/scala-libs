package weco.storage.generators

import weco.Logging
import weco.fixtures.RandomGenerators
import weco.json.JsonUtil._
import weco.storage.IdentityKey
import weco.storage.streaming.Codec
import weco.storage.streaming.Codec._

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
