package uk.ac.wellcome.storage.maxima

import uk.ac.wellcome.storage.{Identified, MaximaError}

trait Maxima[QueryParameter, Id, T] {
  type MaxEither = Either[MaximaError, Identified[Id, T]]

  def max(q: QueryParameter): MaxEither
}
