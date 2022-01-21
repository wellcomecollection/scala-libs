package weco.messaging.worker.steps

import weco.messaging.worker.models._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait MonitoringProcessor {
  implicit val ec: ExecutionContext

  def recordEnd(startTime: Instant, result: Result[_]): Future[Result[Unit]]
}
