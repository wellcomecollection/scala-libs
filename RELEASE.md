RELEASE_TYPE: minor

Change our DynamoDB fixture to use a LocalStack container to mock cloud services, instead of the `peopleperhour/dynamodb` container we were using previously.
The `peopleperhour` image is no longer maintained, and prevents us from using newer SNS APIs in our libraries.

Downstream users will need to replace `peopleperhour/dynamodb` with `localstack` in their Docker Compose files.
