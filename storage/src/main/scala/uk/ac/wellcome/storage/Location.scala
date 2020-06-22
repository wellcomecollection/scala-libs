package uk.ac.wellcome.storage

trait Location

trait Prefix[OfLocation] {
  def asLocation(parts: String*): OfLocation
}
