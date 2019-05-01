package uk.ac.wellcome.storage.locking

import java.time.Instant
import java.time.temporal.TemporalAmount

import uk.ac.wellcome.storage.Lock

case class ExpiringLock(
  id: String,
  contextId: String,
  created: Instant,
  expires: Instant) extends Lock[String, String]

object ExpiringLock {
  def create(id: String,
             contextId: String,
             duration: TemporalAmount): ExpiringLock = {
    val created = Instant.now()

    ExpiringLock(
      id = id,
      contextId = contextId,
      created = created,
      expires = created.plus(duration)
    )
  }
}
