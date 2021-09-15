RELEASE_TYPE: minor

This rolls back the version bump in v30.7.0.
This has led to SQS-backed workers getting stuck with errors like:

    Caused by: software.amazon.awssdk.core.exception.SdkClientException:
    Unable to execute HTTP request: The channel was closed. This may have been
    done by the client (e.g. because the request was aborted), by the service
    (e.g. because there was a handshake error, the request took too long, or
    the client tried to write on a read-only socket), or by an intermediary
    party (e.g. because the channel was idle for too long).

I suspect this was caused by an unexpected upgrade of the AWS SDK, from 2.14.22 (in [Scanamo v1.0-M14](https://github.com/scanamo/scanamo/blob/v1.0-M14/build.sbt#L125)) to 2.15.78 (in [Scanamo v1.0.0-M15](https://github.com/scanamo/scanamo/blob/v1.0.0-M15/build.sbt#L125)).
We're probably missing a configuration option that's required in the newer version, or something has gone bang in the dependency resolution.

This change didn't have the compilation improvement I was hoping for with the Circe/Shapeless change, so I'm rolling it back for now.
