package uk.ac.wellcome.monitoring

trait Metrics[F[_]] {
  def incrementCount(metricName: String): F[Unit]
  def recordValue(metricName: String, value: Double): F[Unit]
}
