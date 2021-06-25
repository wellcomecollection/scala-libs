package uk.ac.wellcome.storage.locking.dynamo

import uk.ac.wellcome.storage.locking.Lock

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.duration.Duration

case class ExpiringLock(id: String,
                        contextId: UUID,
                        created: Instant,
                        expires: Instant)
    extends Lock[String, UUID]

object ExpiringLock {
  def create(id: String, contextId: UUID, duration: Duration): ExpiringLock = {
    val created = Instant.now()

    ExpiringLock(
      id = id,
      contextId = contextId,
      created = created,
      expires = created.plus(duration.toSeconds, ChronoUnit.SECONDS)
    )
  }
}
