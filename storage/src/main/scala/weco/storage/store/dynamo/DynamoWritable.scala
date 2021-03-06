package weco.storage.store.dynamo

import org.scanamo.query._
import org.scanamo.syntax._
import org.scanamo.{ConditionNotMet, DynamoFormat, Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import weco.storage.{Identified, RetryableError, StoreWriteError, Version}
import weco.storage.dynamo.{DynamoHashEntry, DynamoHashRangeEntry}
import weco.storage.store.Writable

import scala.util.{Failure, Success, Try}

sealed trait DynamoWritable[Ident, EntryType, T] extends Writable[Ident, T] {

  protected val client: DynamoDbClient
  protected val table: Table[EntryType]

  protected def parseEntry(entry: EntryType): T
  protected def createEntry(id: Ident, t: T): EntryType

  protected def tableGiven(id: Ident): ConditionalOperation[EntryType, _]

  override def put(id: Ident)(t: T): WriteEither = {
    val entry = createEntry(id, t)

    val ops = tableGiven(id).put(entry)

    Try(Scanamo(client).exec(ops)) match {
      case Success(Right(_)) => Right(Identified(id, parseEntry(entry)))
      case Success(Left(err: ConditionNotMet)) =>
        Left(new StoreWriteError(err.e) with RetryableError)
      case Success(Left(err)) =>
        Left(StoreWriteError(new Throwable(s"Error from Scanamo: $err")))
      case Failure(err: ConditionalCheckFailedException) =>
        Left(new StoreWriteError(err) with RetryableError)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }
}

trait DynamoHashWritable[HashKey, V, T]
    extends DynamoWritable[
      Version[HashKey, V],
      DynamoHashEntry[HashKey, V, T],
      T] {
  implicit protected val formatV: DynamoFormat[V]
  assert(formatV != null)

  override protected def parseEntry(entry: DynamoHashEntry[HashKey, V, T]): T =
    entry.payload

  override protected def createEntry(id: Version[HashKey, V],
                                     t: T): DynamoHashEntry[HashKey, V, T] =
    DynamoHashEntry(id.id, id.version, t)

  override protected def tableGiven(id: Version[HashKey, V])
    : ConditionalOperation[DynamoHashEntry[HashKey, V, T], _] =
    table.when(
      not(attributeExists("id")) or
        (attributeExists("id") and "version" < id.version)
    )
}

trait DynamoHashRangeWritable[HashKey, RangeKey, T]
    extends DynamoWritable[
      Version[HashKey, RangeKey],
      DynamoHashRangeEntry[HashKey, RangeKey, T],
      T] {
  implicit protected val formatRangeKey: DynamoFormat[RangeKey]
  assert(formatRangeKey != null)

  override protected def parseEntry(
    entry: DynamoHashRangeEntry[HashKey, RangeKey, T]): T =
    entry.payload

  override protected def createEntry(
    id: Version[HashKey, RangeKey],
    t: T): DynamoHashRangeEntry[HashKey, RangeKey, T] =
    DynamoHashRangeEntry(id.id, id.version, t)

  override protected def tableGiven(id: Version[HashKey, RangeKey])
    : ConditionalOperation[DynamoHashRangeEntry[HashKey, RangeKey, T], _] =
    table.when(
      not(attributeExists("id"))
    )
}
