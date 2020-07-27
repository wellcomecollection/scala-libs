package uk.ac.wellcome.storage

import java.nio.file.Paths

trait Prefix[OfLocation <: Location] {
  def pathPrefix: String

  def basename: String = Paths.get(pathPrefix).getFileName.toString

  protected def parentOf(path: String): String =
    Paths.get(path).getParent.toString

  def parent: Prefix[OfLocation]

  def asLocation(parts: String*): OfLocation
}
