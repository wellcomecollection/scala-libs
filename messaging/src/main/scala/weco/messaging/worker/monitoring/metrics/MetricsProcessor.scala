package weco.messaging.worker.monitoring.metrics

import weco.messaging.worker.models._
import weco.monitoring.Metrics

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class MetricsProcessor(val namespace: String) {
  def recordResult(
    result: Result[_],
    startTime: Instant
  )(
    implicit metrics: Metrics[Future],
    ec: ExecutionContext
  ): Future[Unit] = {
    val futures = Seq(
      metrics.incrementCount(s"$namespace/${result.name}"),
      metrics.recordValue(s"$namespace/Duration", secondsSince(startTime))
    )

    Future.sequence(futures).map(_ => ())
  }

  private def secondsSince(startTime: Instant): Long =
    Duration
      .between(startTime, Instant.now())
      .getSeconds
}
