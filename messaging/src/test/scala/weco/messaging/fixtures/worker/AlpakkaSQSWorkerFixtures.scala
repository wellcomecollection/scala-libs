package weco.messaging.fixtures.worker

import akka.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.messaging.fixtures.SQS
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.sqsworker.alpakka.{
  AlpakkaSQSWorker,
  AlpakkaSQSWorkerConfig
}
import weco.messaging.worker.monitoring.metrics.MetricsMonitoringProcessor
import weco.monitoring.MetricsConfig
import weco.monitoring.memory.MemoryMetrics

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait AlpakkaSQSWorkerFixtures
    extends WorkerFixtures
    with Matchers
    with SQS {

  def createAlpakkaSQSWorkerConfig(queue: Queue,
                                   namespace: String = randomAlphanumeric())
    : AlpakkaSQSWorkerConfig =
    AlpakkaSQSWorkerConfig(
      metricsConfig = MetricsConfig(namespace, flushInterval = 1.second),
      sqsConfig = createSQSConfigWith(queue)
    )

  def withAlpakkaSQSWorker[R](
    queue: Queue,
    process: TestInnerProcess,
    namespace: String = randomAlphanumeric()
  )(testWith: TestWith[
      (AlpakkaSQSWorker[MyWork, MyContext, MyContext, MySummary],
       AlpakkaSQSWorkerConfig,
       MemoryMetrics,
       CallCounter),
      R])(implicit
          as: ActorSystem,
          ec: ExecutionContext): R = {
      implicit val metrics: MemoryMetrics = new MemoryMetrics()

      val metricsProcessorBuilder
        : ExecutionContext => MetricsMonitoringProcessor[MyWork] =
        new MetricsMonitoringProcessor[MyWork](namespace)(metrics, _)

      val config = createAlpakkaSQSWorkerConfig(queue, namespace)

      val callCounter = new CallCounter()
      val testProcess = (work: MyWork) => createResult(process, callCounter)(ec)(work)

      val worker =
        new AlpakkaSQSWorker[MyWork, MyContext, MyContext, MySummary](
          config,
          metricsProcessorBuilder)(testProcess)

      testWith((worker, config, metrics, callCounter))
    }
}
