package weco.messaging.fixtures.monitoring.tracing

import io.opentracing.mock.MockSpan
import io.opentracing.{Span, Tracer}
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.messaging.worker.monitoring.tracing.{
  OpenTracingMonitoringProcessor,
  OpenTracingSpanSerializer
}
import weco.messaging.worker.monitoring.tracing.{
  OpenTracingMonitoringProcessor,
  OpenTracingSpanSerializer
}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

trait TracingFixtures extends Matchers { this: Suite =>

  def withOpenTracingMetricsProcessor[MyWork, R](
    MapContextCarrier: OpenTracingSpanSerializer[Map[String, String]],
    tracer: Tracer)(
    testWith: TestWith[
      OpenTracingMonitoringProcessor[MyWork, Map[String, String]],
      R]): R = {
    val processor =
      new OpenTracingMonitoringProcessor[MyWork, Map[String, String]](
        "namespace")(
        tracer,
        ExecutionContext.Implicits.global,
        MapContextCarrier)
    testWith(processor)
  }

  def spanShouldBeTaggeddWith(span: Span,
                              exception: Throwable,
                              failureType: String) = {
    span
      .asInstanceOf[MockSpan]
      .tags()
      .asScala
      .toMap should contain only (("error" -> true), "error.type" -> failureType)
    val logEntries = span
      .asInstanceOf[MockSpan]
      .logEntries()
      .asScala
      .map(_.fields().asScala.toMap)
    logEntries should contain only (Map(
      ("event" -> "error"),
      ("error.object" -> exception)))
  }

}
