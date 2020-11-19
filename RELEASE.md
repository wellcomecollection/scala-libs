RELEASE_TYPE: minor

The `DynamoReadable` trait (and all extensions of it) now have a new field `consistencyMode`, which allows you to decide between:

*   **Eventually consistent reads**, which might not return data from a recent write operation
*   **Strongly consistent reads**, whcih always return the most recent data, but with higher latency and using more throughput capacity

The default is eventual consistency, which is the existing behaviour.