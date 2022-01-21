package weco.messaging.fixtures.worker

import weco.messaging.worker._
import weco.messaging.worker.models._
import weco.messaging.worker.monitoring.metrics.MetricsRecorder

import scala.concurrent.{ExecutionContext, Future}

trait WorkerFixtures {
  type MySummary = String
  type TestResult = Result[MySummary]
  type TestInnerProcess = MyWork => TestResult
  type TestProcess = MyWork => Future[TestResult]

  case class MyMessage(s: String)
  case class MyWork(s: String)

  object MyWork {
    def apply(message: MyMessage): MyWork =
      new MyWork(message.s)
  }

  def messageToWork(shouldFail: Boolean): MyMessage => Either[RuntimeException, MyWork] =
    (message: MyMessage) =>
      Either.cond(
        !shouldFail,
        right = MyWork(message),
        left = new RuntimeException("messageToWork goes BOOM!")
      )

  def actionToAction(toActionShouldFail: Boolean)(result: Result[MySummary])(
    implicit ec: ExecutionContext): Future[MyExternalMessageAction] = Future {
    if (toActionShouldFail) {
      throw new RuntimeException("BOOM")
    } else {
      MyExternalMessageAction(result)
    }
  }

  case class MyExternalMessageAction(result: Result[MySummary])

  class MyProcessor(
    val metricsRecorder: MetricsRecorder,
    testProcess: TestInnerProcess,
    val parseMessage: MyMessage => Either[Throwable, MyWork]
  )(implicit val ec: ExecutionContext)
      extends Processor[MyMessage, MyWork, MySummary, MyExternalMessageAction] {

    val callCounter = new CallCounter()

    override val retryAction: MyMessage => MyExternalMessageAction =
      _ => MyExternalMessageAction(
        NonDeterministicFailure[MySummary](new Throwable("BOOM!"))
      )

    override val completedAction: MyMessage => MyExternalMessageAction =
      _ => MyExternalMessageAction(Successful())

    override val doProcessing =
      (work: MyWork) => createResult(testProcess, callCounter)(ec)(work)
  }

  val message = MyMessage("some_content")
  val work = MyWork("some_content")

  class CallCounter() {
    var calledCount = 0
  }

  def createResult(op: TestInnerProcess, callCounter: CallCounter)(
    implicit ec: ExecutionContext): MyWork => Future[TestResult] = {

    (work: MyWork) =>
      {
        callCounter.calledCount = callCounter.calledCount + 1

        Future(op(work))
      }
  }

  val successful = (_: MyWork) => {
    Successful[MySummary](
      Some("Summary Successful")
    )
  }

  val nonDeterministicFailure = (_: MyWork) =>
    NonDeterministicFailure[MySummary](
      new RuntimeException("NonDeterministicFailure"),
      Some("Summary NonDeterministicFailure")
  )

  val deterministicFailure = (_: MyWork) =>
    DeterministicFailure[MySummary](
      new RuntimeException("DeterministicFailure"),
      Some("Summary DeterministicFailure")
  )

  val monitoringProcessorFailure = (_: MyWork) =>
    MonitoringProcessorFailure[MySummary](
      new RuntimeException("MonitoringProcessorFailure"),
      Some("Summary MonitoringProcessorFailure")
  )

  val exceptionState = (_: MyWork) => {
    throw new RuntimeException("BOOM")

    Successful[MySummary](Some("exceptionState"))
  }
}
