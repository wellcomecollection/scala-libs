package uk.ac.wellcome.messaging.worker.monitoring.metrics

import java.time.{Duration, Instant}

import uk.ac.wellcome.messaging.worker.models._
import uk.ac.wellcome.monitoring.Metrics

import scala.concurrent.{ExecutionContext, Future}

trait MetricsProcessor {

  val namespace: String

  private def metricName(name: String) = s"$namespace/$name"

  final def metric[ProcessMonitoringClient <: MetricsMonitoringClient](
    result: Result[_],
    startTime: Instant
  )(
    implicit monitoringClient: ProcessMonitoringClient,
    ec: ExecutionContext
  ): Future[Unit] = {
    val resultName = result match {
      case _: Successful[_]                 => "Successful"
      case _: DeterministicFailure[_]       => "DeterministicFailure"
      case _: NonDeterministicFailure[_]    => "NonDeterministicFailure"
      case _: MonitoringProcessorFailure[_] => "MonitoringProcessorFailure"
    }

    val countResult = monitoringClient.incrementCount(metricName(resultName))

    val recordDuration =
      monitoringClient.recordValue(
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
