package weco.messaging.sqs

trait RecognisedFailure extends Exception { self: Throwable =>
  val message: String = self.getMessage
}
