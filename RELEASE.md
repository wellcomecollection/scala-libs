RELEASE_TYPE: major

### Libraries affected

messaging, monitoring

### Description

*   Metrics.recordValue() no longer takes an optional `metricUnit` parameter, because none of our code was using it.
*   MetricsMonitoringClient has been removed.  Any uses of it can be replaced by an instance of Metrics[Future].
