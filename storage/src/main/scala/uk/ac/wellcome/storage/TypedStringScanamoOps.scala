package uk.ac.wellcome.storage

import org.scanamo.DynamoFormat
import weco.json.{TypedString, TypedStringOps}

trait TypedStringScanamoOps[T <: TypedString[_]] extends TypedStringOps[T] {
  implicit def evidence: DynamoFormat[T] =
    DynamoFormat
      .coercedXmap[T, String, IllegalArgumentException](
        apply,
        _.underlying
      )
}
