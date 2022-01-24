package weco.messaging.worker.models

sealed trait Result[Summary] {
  val summary: Option[Summary]

  def name: String =
    this.getClass.getSimpleName

  def pretty: String =
    s"$name: ${summary.getOrElse("<no-summary>")}"
}

case class DeterministicFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]
    with Completed

case class NonDeterministicFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]
    with Retry

case class Successful[Summary](
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]
    with Completed

case class MonitoringProcessorFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]
    with Completed
