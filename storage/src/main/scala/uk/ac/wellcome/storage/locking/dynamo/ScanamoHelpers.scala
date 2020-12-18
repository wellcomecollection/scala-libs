package uk.ac.wellcome.storage.locking.dynamo

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  ConditionalCheckFailedException,
  PutItemResult
}
import org.scanamo.ops.ScanamoOps
import org.scanamo.{DynamoFormat, Scanamo, Table => ScanamoTable}

import scala.util.Try

trait ScanamoHelpers[T] {
  type ScanamoPutItemResult =
    Either[ConditionalCheckFailedException, PutItemResult]
  type ScanamoPut =
    ScanamoOps[ScanamoPutItemResult]
  type SafeEither[Out] =
    Either[Throwable, Out]

  implicit val df: DynamoFormat[T]

  protected val client: DynamoDbClient
  val scanamo = Scanamo(client)

  protected val table: ScanamoTable[T]
  protected val index: String

  protected def safePutItem(ops: ScanamoPut): SafeEither[PutItemResult] =
    for {
      either <- toEither(scanamo.exec(ops))
      result <- either
    } yield result

  protected def toEither[Out](f: => Out): Either[Throwable, Out] =
    Try(f).toEither
}
