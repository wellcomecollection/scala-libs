package uk.ac.wellcome.storage.locking.dynamo

import org.scanamo.{DynamoFormat, Scanamo, Table => ScanamoTable}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

import scala.util.Try

trait ScanamoHelpers[T] {
  implicit val df: DynamoFormat[T]

  protected val client: DynamoDbClient
  val scanamo = Scanamo(client)

  protected val table: ScanamoTable[T]
  protected val index: String

  protected def toEither[Out](f: => Out): Either[Throwable, Out] =
    Try(f).toEither
}
