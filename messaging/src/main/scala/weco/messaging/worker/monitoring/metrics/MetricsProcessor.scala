package weco.messaging.worker.monitoring.metrics

import java.time.{Duration, Instant}

import weco.messaging.worker.models._
import weco.monitoring.Metrics

import scala.concurrent.{ExecutionContext, Future}

class MetricsProcessor(val namespace: String) {
  private def metricName(name: String) = s"$namespace/$name"

  def recordResult(
    result: Result[_],
    startTime: Instant
  )(
    implicit metrics: Metrics[Future],
    ec: ExecutionContext
  ): Future[Unit] = {
    val resultName = result match {
      case _: Successful[_]                 => "Successful"
      case _: DeterministicFailure[_]       => "DeterministicFailure"
      case _: NonDeterministicFailure[_]    => "NonDeterministicFailure"
      case _: MonitoringProcessorFailure[_] => "MonitoringProcessorFailure"
    }

    val countResult = metrics.incrementCount(metricName(resultName))

    val recordDuration =
      metrics.recordValue(
        metricName("Duration"),
        Duration
          .between(
            startTime,
            Instant.now()
          )
          .getSeconds
      )

    Future
      .sequence(
        List(countResult, recordDuration)
      )
      .map(_ => ())
  }
}
