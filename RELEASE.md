RELEASE_TYPE: patch

No user-facing changes. Splits the build steps in CI for the scala-libs repo to increase potential parallelism in builds, and to make it clearer where failures occur.