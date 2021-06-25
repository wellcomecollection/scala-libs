package weco.messaging.sqs

case class SQSConfig(queueUrl: String, parallelism: Integer = 10)
