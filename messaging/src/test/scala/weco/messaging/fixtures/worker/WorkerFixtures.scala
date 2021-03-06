package weco.messaging.fixtures.worker

import java.time.Instant

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import weco.messaging.worker._
import weco.messaging.worker.models._
import weco.messaging.worker.monitoring.metrics.MetricsMonitoringProcessor
import weco.messaging.worker.steps.MessageProcessor

import scala.concurrent.{ExecutionContext, Future}

trait WorkerFixtures extends Matchers {
  type MySummary = String
  type MyContext = Instant
  type TestResult = Result[MySummary]
  type TestInnerProcess = MyWork => TestResult
  type TestProcess = MyWork => Future[TestResult]

  case class MyMessage(s: String)
  case class MyWork(s: String)

  object MyWork {
    def apply(message: MyMessage): MyWork =
      new MyWork(message.s)
  }

  def messageToWork(shouldFail: Boolean)(message: MyMessage)
    : (Either[Throwable, MyWork], Either[Throwable, Option[MyContext]]) =
    if (shouldFail) {
      (Left(new RuntimeException("BOOM")), Right(None))
    } else {
      (Right(MyWork(message)), Right(None))
    }

  def actionToAction(toActionShouldFail: Boolean)(result: Result[MySummary])(
    implicit ec: ExecutionContext): Future[MyExternalMessageAction] = Future {
    if (toActionShouldFail) {
      throw new RuntimeException("BOOM")
    } else {
      MyExternalMessageAction(result.asInstanceOf[Action])
    }
  }

  case class MyExternalMessageAction(action: Action)

  class MyWorker(
    val monitoringProcessor: MetricsMonitoringProcessor[MyWork],
    testProcess: TestInnerProcess,
    val transform: MyMessage => (Either[Throwable, MyWork],
                                 Either[Throwable, Option[MyContext]])
  )(implicit val ec: ExecutionContext)
      extends Worker[
        MyMessage,
        MyWork,
        MyContext,
        MyContext,
        MySummary,
        MyExternalMessageAction
      ] {

    val callCounter = new CallCounter()

    override val retryAction: MessageAction =
      _ => MyExternalMessageAction(new Retry {})

    override val completedAction: MessageAction =
      _ => MyExternalMessageAction(new Completed {})

    override val doWork =
      (work: MyWork) => createResult(testProcess, callCounter)(ec)(work)

    override type Completion = WorkCompletion[MyMessage, MySummary]
  }

  class MyMessageProcessor(
    testProcess: TestProcess
  ) extends MessageProcessor[MyWork, MySummary] {

    override protected val doWork: TestProcess =
      testProcess
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

  val shouldBeSuccessful: Result[_] => Assertion =
    (r: Result[_]) => r shouldBe a[Successful[_]]
  val shouldBeDeterministicFailure: Result[_] => Assertion =
    (r: Result[_]) => r shouldBe a[DeterministicFailure[_]]
  val shouldBeNonDeterministicFailure: Result[_] => Assertion =
    (r: Result[_]) => r shouldBe a[NonDeterministicFailure[_]]
  val shouldBeMonitoringProcessorFailure: Result[_] => Assertion =
    (r: Result[_]) => r shouldBe a[MonitoringProcessorFailure[_]]

  val shouldBeCompleted: Result[_] => Assertion = (r: Result[_]) =>
    r shouldBe a[Completed]
  val shouldBeRetry: Result[_] => Assertion = (r: Result[_]) =>
    r shouldBe a[Retry]
}
