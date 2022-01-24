package weco.messaging.worker.steps

import weco.messaging.worker.models.{DeterministicFailure, Result}
import weco.messaging.worker.models.Result

import scala.concurrent.{ExecutionContext, Future}

/***
  * Executes some operation on a `Work` and returns a [[Result]]
  * with a optional descriptive `Summary`
  */
trait MessageProcessor[Work, Summary] {
  type ResultSummary = Future[Result[Summary]]

  protected val doWork: (Work) => ResultSummary

  final def process(workEither: Either[Throwable, Work])(
    implicit ec: ExecutionContext): Future[Result[Summary]] = workEither.fold(
    e => Future.successful(DeterministicFailure[Summary](e)),
    w =>
      doWork(w) recover {
        case e => DeterministicFailure[Summary](e)
      }
  )
}
