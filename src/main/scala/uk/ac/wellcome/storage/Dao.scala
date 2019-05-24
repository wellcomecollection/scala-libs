package uk.ac.wellcome.storage

trait Dao[Ident, T] {
  def get(id: Ident): Either[ReadError, T]
  def put(t: T): Either[WriteError, Unit]
}
