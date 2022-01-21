package weco.messaging.worker.steps

import weco.messaging.worker.models._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait MonitoringProcessor {
  implicit val ec: ExecutionContext

  def recordEnd[Recorded](
    startTime: Instant,
    result: Result[Recorded]): Future[Result[Unit]]
}
