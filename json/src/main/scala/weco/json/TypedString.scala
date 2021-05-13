package weco.json

import io.circe.{Decoder, Encoder, HCursor, Json}

/** In a number of cases, we have "meaningful" strings -- i.e., strings where the
  * value has some meaning, and isn't just opaque text.
  *
  * e.g. a canonical ID in the catalogue, an external identifier in the storage
  * service
  *
  * Within our code, it's useful to create a special type for these strings,
  * so we get the benefit of the type checker.  We could use a case class, e.g.
  *
  *     case class Shape(name: String)
  *
  * but then we'll get the automatic Circe case class JSON encoder/decoder, which
  * adds an unnecessary layer of indirection:
  *
  *     {"name": "square"}
  *
  * we would rather encode/decode these to raw strings in JSON, which is slightly
  * easier to work with.  It also retains backwards compatibility with old data,
  * if we "upgrade" a value from `String` to a `TypedString[_]`.
  *
  * You can define custom encoders/decoders on a regular case class, but then you
  * have to make sure they're in scope.  If you get this wrong, you get mismatched
  * serialisations, which is a world of pain.
  *
  * This class allows you to create "meaningful" strings that serialise to raw strings
  * in JSON.  It also handles some of the things you get for "free" with case classes,
  * e.g. equality and hashing.
  *
  *     class Shape(val underlying: String) extends TypedString[Shape]
  *
  *     object Shape extends TypedStringOps[Shape] {
  *       override def apply(name: String): Shape = new Shape(name)
  *     }
  *
  * See also: TypedStringScanamoOps in the storage lib, which provides a DynamoDB format.
  *
  */
trait TypedString[T <: TypedString[_]] {
  val underlying: String

  override def toString: String = underlying

  def canEqual(a: Any): Boolean = a.getClass == this.getClass

  override def equals(that: Any): Boolean =
    that match {
      case that if this.canEqual(that) =>
        this.underlying == that.asInstanceOf[T].underlying
      case _ => false
    }

  override def hashCode: Int = underlying.hashCode
}

trait TypedStringOps[T <: TypedString[_]] {
  def apply(underlying: String): T

  implicit val encoder: Encoder[T] =
    (value: T) => Json.fromString(value.toString)

  implicit val decoder: Decoder[T] = (cursor: HCursor) =>
    cursor.value.as[String].map(apply)
}
