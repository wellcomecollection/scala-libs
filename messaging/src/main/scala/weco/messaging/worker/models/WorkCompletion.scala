package weco.messaging.worker.models

case class WorkCompletion[Message, Summary](message: Message,
                                            summary: Result[Summary])
