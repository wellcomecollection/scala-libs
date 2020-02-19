package uk.ac.wellcome.messaging.worker.monitoring.tracing

import io.opentracing.contrib.concurrent.TracedExecutionContext
import io.opentracing.{Span, SpanContext, Tracer}
import uk.ac.wellcome.messaging.worker.models._
import uk.ac.wellcome.messaging.worker.steps.MonitoringProcessor

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait ContextCarrier[T]{
  def inject(tracer: Tracer, span: SpanContext): T

  def extract(tracer: Tracer, t:T): SpanContext
}

class OpenTracingMonitoringProcessor[Work](namespace: String)(tracer: Tracer,wrappedEc:ExecutionContext,carrier: ContextCarrier[Map[String,String]])
    extends MonitoringProcessor[Work, Map[String,String],Span] {

  override implicit val ec: ExecutionContext = new TracedExecutionContext(wrappedEc, tracer)

  override def recordStart(work: Either[Throwable, Work],
                           context: Either[Throwable, Option[Map[String,String]]]): Future[Either[Throwable, Span]] = {
    val f = Future {
      val span = context match {
        case Right(None) =>
          val span = tracer.buildSpan(namespace).start()
          span
        case Right(Some(c)) =>
          val rootContext = carrier.extract(tracer, c)
          val span = tracer.buildSpan(namespace).asChildOf(rootContext).start()
          span
        case Left(ex) => val span = tracer.buildSpan(namespace).start()
          span.setTag("error", true)
          span.setTag("error.type", classOf[DeterministicFailure[_]].getSimpleName)
          span.log(Map("event" -> "error", "error.object" -> ex).asJava)
          span
      }
      work match {
        case Right(_) =>
        case Left(ex) =>
          span.setTag("error", true)
          span.setTag("error.type", classOf[DeterministicFailure[_]].getSimpleName)
          span.log(Map("event" -> "error", "error.object" -> ex).asJava)
      }
      Right(span)

    }
    f recover { case e =>
      Left(e)
    }
  }

  override def recordEnd[Recorded]( span: Either[Throwable,Span],
                                   result: Result[Recorded]): Future[Result[Unit]] = {
    val f: Future[Result[Unit]] = Future {
      span.fold(throwable => MonitoringProcessorFailure(throwable), span => {
        result match {
          case Successful(_) =>
          case f@DeterministicFailure(failure, _) =>
            span.setTag("error", true)
            span.setTag("error.type", f.getClass.getSimpleName)
            span.log(Map("event" -> "error", "error.object" -> failure).asJava)
          case f@NonDeterministicFailure(failure, _) =>
            span.setTag("error", true)
            span.setTag("error.type", f.getClass.getSimpleName)
            span.log(Map("event" -> "error", "error.object" -> failure).asJava)
          case _ =>
        }
        span.finish()
        Successful[Unit](None)
      }
      )
    }
    f recover { case e =>
      MonitoringProcessorFailure[Unit](e, None)
    }
  }
}