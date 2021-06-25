package weco.storage.maxima

import weco.storage.{Identified, MaximaError}

trait Maxima[QueryParameter, Id, T] {
  type MaxEither = Either[MaximaError, Identified[Id, T]]

  def max(q: QueryParameter): MaxEither
}
