package weco.messaging.worker.models

sealed trait Result[Summary] {
  val summary: Option[Summary]
  def pretty =
    s"${this.getClass.getSimpleName}: ${summary.getOrElse("<no-summary>")}"
}

case class TerminalFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]

case class RetryableFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]

case class Successful[Summary](
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]

case class MonitoringProcessorFailure[Summary](
  failure: Throwable,
  summary: Option[Summary] = Option.empty[Summary]
) extends Result[Summary]
