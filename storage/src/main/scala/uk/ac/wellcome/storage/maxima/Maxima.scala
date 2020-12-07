package uk.ac.wellcome.storage.maxima

import uk.ac.wellcome.storage.{Identified, ReadError}

trait Maxima[QueryParameter, Id, T] {
  type MaxEither = Either[ReadError, Identified[Id, T]]

  def max(q: QueryParameter): MaxEither
}
