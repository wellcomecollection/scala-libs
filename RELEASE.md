RELEASE_TYPE: major

This release tidies up the random generators provided by `RandomGenerators`.  This trait should be the canonical source of random data for tests in the platform, rather than multiple implementations of very similar functions copy/pasted into different codebases.