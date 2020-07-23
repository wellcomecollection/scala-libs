package uk.ac.wellcome.storage

trait Prefix[OfLocation <: Location] {
  def asLocation(parts: String*): OfLocation
}
