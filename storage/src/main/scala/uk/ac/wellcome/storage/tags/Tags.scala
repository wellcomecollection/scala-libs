package uk.ac.wellcome.storage.tags

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.{Readable, Updatable}

trait Tags[Ident]
    extends Readable[Ident, Map[String, String]]
    with Updatable[Ident, Map[String, String]]
    with Logging {

  protected def writeTags(
    id: Ident,
    tags: Map[String, String]): Either[WriteError, Map[String, String]]

  def update(id: Ident)(updateFunction: UpdateFunction): UpdateEither = {
    info(s"Tags on $id: updating tags")

    for {
      existingTags <- get(id) match {
        case Right(value)                 => Right(value)
        case Left(err: DoesNotExistError) => Left(UpdateNoSourceError(err))
        case Left(err)                    => Left(UpdateReadError(err))
      }

      _ = debug(s"Tags on $id: existing tags = $existingTags")

      newTags <- updateFunction(existingTags.identifiedT)

      _ = debug(s"Tags on $id: new tags      = $newTags")

      result <- if (newTags == existingTags.identifiedT) {
        debug(s"Tags on $id: no change, so skipping a write")
        Right(existingTags)
      } else {
        debug(s"Tags on $id: tags have changed, so writing new tags")
        writeTags(id = id, tags = newTags) match {
          case Right(value) => Right(Identified(id, value))
          case Left(err) => {
            warn(s"Error when trying to write tags to $id: $err")
            Left(UpdateWriteError(err))
          }
        }
      }
    } yield result
  }
}
