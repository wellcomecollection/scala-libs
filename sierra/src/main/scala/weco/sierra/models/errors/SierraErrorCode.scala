package weco.sierra.models.errors

case class SierraErrorCode(
  code: Int,
  specificCode: Int,
  httpStatus: Int,
  name: String,
  description: Option[String] = None
)
