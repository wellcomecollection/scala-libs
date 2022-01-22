package weco.messaging.worker.monitoring.metrics

import java.time.{Duration, Instant}

import weco.messaging.worker.models._
import weco.monitoring.Metrics

import scala.concurrent.{ExecutionContext, Future}

class MetricsProcessor(val namespace: String)(implicit metrics: Metrics[Future], ec: ExecutionContext) {
  def recordResult(
    result: Result[_],
    startTime: Instant
  ): Future[Unit] = {
    val futures = List(
      metrics.incrementCount(s"$namespace/${result.getClass.getSimpleName}"),
      metrics.recordValue(s"$namespace/Duration", secondsSince(startTime))
    )

    Future.sequence(futures).map(_ => ())
  }

  private def secondsSince(startTime: Instant): Long =
    Duration
      .between(startTime, Instant.now())
      .getSeconds
}
