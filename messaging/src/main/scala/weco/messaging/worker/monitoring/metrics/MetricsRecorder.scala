package weco.messaging.worker.monitoring.metrics

import weco.messaging.worker.models._
import weco.monitoring.Metrics

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class MetricsRecorder(namespace: String)(implicit
                                         metrics: Metrics[Future],
                                         ec: ExecutionContext) {

  def recordEnd(startTime: Instant, result: Result[_]): Future[Result[Unit]] = {
    val r = recordDurationMetric(result, startTime)
      .map(_ => Successful[Unit]())

    r recover {
      case e => MonitoringProcessorFailure[Unit](e)
    }
  }

  private def metricName(name: String) = s"$namespace/$name"

  private def recordDurationMetric(result: Result[_],
                                   startTime: Instant): Future[Unit] = {
    val resultName = result match {
      case _: Successful[_]                 => "Successful"
      case _: TerminalFailure[_]            => "DeterministicFailure"
      case _: RetryableFailure[_]           => "NonDeterministicFailure"
      case _: MonitoringProcessorFailure[_] => "MonitoringProcessorFailure"
    }

    val futures = List(
      metrics.incrementCount(metricName(resultName)),
      metrics.recordValue(metricName("Duration"), secondsSince(startTime))
    )

    Future.sequence(futures).map(_ => ())
  }

  private def secondsSince(startTime: Instant) =
    Duration
      .between(startTime, Instant.now())
      .getSeconds
}
