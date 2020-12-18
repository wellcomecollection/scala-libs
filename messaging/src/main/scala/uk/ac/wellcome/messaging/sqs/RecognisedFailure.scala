package uk.ac.wellcome.messaging.sqs

trait RecognisedFailure extends Exception { self: Exception =>
  val message: String = self.getMessage
}
