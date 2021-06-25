package weco.storage.store

import weco.storage._
import weco.storage.{Identified, WriteError}

trait Store[Ident, T] extends Readable[Ident, T] with Writable[Ident, T]

trait Readable[Ident, T] {
  type ReadEither = Either[ReadError, Identified[Ident, T]]

  def get(id: Ident): ReadEither
}

trait Writable[Ident, T] {
  type WriteEither = Either[WriteError, Identified[Ident, T]]

  def put(id: Ident)(t: T): WriteEither
}

trait Updatable[Ident, T] {
  type UpdateEither = Either[UpdateError, Identified[Ident, T]]
  type UpdateFunction = T => Either[UpdateFunctionError, T]

  def update(id: Ident)(updateFunction: UpdateFunction): UpdateEither
}
