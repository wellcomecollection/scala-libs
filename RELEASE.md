RELEASE_TYPE: minor

Change our SNS and SQS fixture to use a LocalStack container to mock cloud services, instead of the `fake-sns` and `elasticmq` containers we were using previously.
The `fake-sns` image is no longer maintained, and prevents us from using newer SNS APIs in our libraries.

Downstream users will need to replace `elasticmq` with `localstack` in their Docker Compose files.
