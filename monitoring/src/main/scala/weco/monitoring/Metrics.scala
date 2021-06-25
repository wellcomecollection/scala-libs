package weco.monitoring

import scala.language.higherKinds

trait Metrics[F[_]] {
  def incrementCount(metricName: String): F[Unit]
  def recordValue(metricName: String, value: Double): F[Unit]
}
