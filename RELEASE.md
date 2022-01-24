RELEASE_TYPE: minor

This release tidies up the `Result` types to reflect the desired behaviour (TerminalFailure, RetryableFailure, Successful) rather than their properties (Deterministic, NonDeterministic).
This will need to be changed in downstream code.

Additionally, the full body of any message deleted as a TerminalFailure will be logged, to make it easier to retry/redrive these messages later.