package weco.messaging.worker.models

sealed trait Action

trait Completed extends Action
trait Retry extends Action
