package uk.ac.wellcome.storage

trait Location {
  def join(parts: String*): Location
}

trait Prefix[OfLocation <: Location] {
  def asLocation(parts: String*): OfLocation
}
