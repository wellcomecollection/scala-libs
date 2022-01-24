RELEASE_TYPE: minor

AlpakkaSQSWorker will retry a `TerminalFailure` if it determines that the error was a flaky error from somewhere in AWS, e.g. a DNS resolution error.
