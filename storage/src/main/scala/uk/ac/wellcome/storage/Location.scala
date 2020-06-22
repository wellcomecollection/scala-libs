package uk.ac.wellcome.storage

trait Location

trait Prefix[OfLocation <: Location] {
  def asLocation(parts: String*): OfLocation
}
