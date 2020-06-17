package uk.ac.wellcome.storage.tags.memory

import uk.ac.wellcome.storage.{DoesNotExistError, Identified, WriteError}
import uk.ac.wellcome.storage.tags.Tags

class MemoryTags[Ident](initialTags: Map[Ident, Map[String, String]])
    extends Tags[Ident] {
  private var underlying: Map[Ident, Map[String, String]] = initialTags

  override def get(id: Ident): ReadEither =
    synchronized {
      underlying.get(id) match {
        case Some(tags) => Right(Identified(id, tags))
        case None       => Left(DoesNotExistError())
      }
    }

  override protected def writeTags(
    id: Ident,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] =
    synchronized {
      debug(s"MemoryTags.put($id, $tags) before = $underlying")
      underlying = underlying ++ Map(id -> tags)
      debug(s"MemoryTags.put($id, $tags) after  = $underlying")
      Right(tags)
    }
}
