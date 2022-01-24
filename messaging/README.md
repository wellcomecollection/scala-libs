# messaging

This library has classes for inter-app messaging.

Most of our services form a pipeline: an app receives a message, does some work, then sends another message to the next app in line.
This continues until the work is finished.

<img src="pipeline.svg">

This is the [pub/sub (publish-subscribe) pattern](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern).

We use two AWS primitives for messaging:

*   To **receive messages**, apps poll an *SQS queue*. (SQS = Simple Queuing Service)
*   To **send messages**, apps sends notification to an *SNS topic*. (SNS = Simple Notification Service)

## Key components

*   To **receive messages** from SQS, we have two components, both of which provide an Akka stream for processing from SQS:

    -   `SQSStream` is used in the [catalogue pipeline].
        It provides low-level, fine-grained control over the stream, e.g. allowing callers to process multiple messages together.

    -   `AlpakkaSQSWorker` is used in the [storage service].
        It provides a simpler, one-message-at-a-time interface.

    That we have two components is a historical accident.
    In an ideal world, we might have a single component that's used in both projects – but at this point, both are well tested in their specific context, and trying to refactor one of them away would be a significant risk.

*   To **send messages**, we have two components, both based on the `MessageSender` trait:

    -   `MemoryMessageSender` is an in-memory implementation, meant for easy testing of apps that use the `MessageSender` functionality
    -   `SNSMessageSender` sends notifications to SNS, meant for live applications

[catalogue pipeline]: https://github.com/wellcomecollection/catalogue-pipeline
[storage service]: https://github.com/wellcomecollection/storage-service
