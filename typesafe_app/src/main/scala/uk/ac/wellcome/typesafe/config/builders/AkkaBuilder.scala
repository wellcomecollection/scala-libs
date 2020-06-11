package uk.ac.wellcome.typesafe.config.builders

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

object AkkaBuilder {
  implicit val actorSystem: ActorSystem = ActorSystem("main-actor-system")

  def buildActorSystem(): ActorSystem =
    actorSystem

  def buildMaterializer(): Materializer =
    Materializer(actorSystem)

  def buildExecutionContext(): ExecutionContext =
    actorSystem.dispatcher
}
