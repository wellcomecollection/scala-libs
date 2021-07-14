RELEASE_TYPE: minor

This release removes all the use of `SqsClient`, because everywhere else in our projects we've switched exclusively to `SqsAsyncClient`.
