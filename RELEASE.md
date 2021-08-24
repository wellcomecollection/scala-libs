RELEASE_TYPE: minor

This changes the way we inspect queue sizes in the SQS fixture: rather than waiting for the visibility timeout to expire, we inspect the queue metrics for all messages (visible, in-flight, delayed), and compare those to the expected size.

This should make all tests using `assertQueueSize` or `assertQueueEmpty` both more accurate and a lot faster.