package uk.ac.wellcome.messaging.fixtures.worker

import akka.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.fixtures.monitoring.metrics.MetricsFixtures
import uk.ac.wellcome.messaging.sqsworker.alpakka.{
  AlpakkaSQSWorker,
  AlpakkaSQSWorkerConfig
}
import uk.ac.wellcome.messaging.worker.monitoring.metrics.MetricsMonitoringProcessor
import uk.ac.wellcome.monitoring.MetricsConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait AlpakkaSQSWorkerFixtures
    extends WorkerFixtures
    with MetricsFixtures
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
       FakeMetricsMonitoringClient,
       CallCounter),
      R])(implicit
          as: ActorSystem,
          ec: ExecutionContext): R =
    withFakeMonitoringClient(false) { client: FakeMetricsMonitoringClient =>
      val metricsProcessorBuilder
        : ExecutionContext => MetricsMonitoringProcessor[MyWork] =
        new MetricsMonitoringProcessor[MyWork](namespace)(client, _)

      val config = createAlpakkaSQSWorkerConfig(queue, namespace)

      val callCounter = new CallCounter()
      val testProcess = (work: MyWork) => createResult(process, callCounter)(ec)(work)

      val worker =
        new AlpakkaSQSWorker[MyWork, MyContext, MyContext, MySummary](
          config,
          metricsProcessorBuilder)(testProcess)

      testWith((worker, config, client, callCounter))
    }
}
