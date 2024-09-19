# scala-libs

[![Build status](https://badge.buildkite.com/4a84a28feca6865e192e0adaba1c2e33f1e773e58957459c47.svg?branch=main)](https://buildkite.com/wellcomecollection/scala-libraries)

This is a collection of utilities that are shared across our Scala-based repositories ([catalogue-api], [catalogue-pipeline], [storage-service]).

It includes:

*   Code for interacting with AWS services we use across the platform, like Amazon S3 and DynamoDB
*   Test helpers and fixtures that let us write tests in a consistent way
*   Helpers for using [Typesafe] to get config into our applications

This library is meant to increase code reuse among our applications, not to be a general-purpose library.

[catalogue-api]: https://github.com/wellcomecollection/catalogue-api
[catalogue-pipeline]: https://github.com/wellcomecollection/catalogue-pipeline
[storage-service]: https://github.com/wellcomecollection/storage-service
[elastic4s]: https://github.com/sksamuel/elastic4s
[Typesafe]: https://lightbend.github.io/config/


## Usage

If you work at Wellcome:

*   These libraries are already configured in our Scala projects.
    Your IDE should pick up the library as another dependency.

If you don't work at Wellcome:

*   Copy/paste any code you're interested in (and the tests!) into your codebase, and adding a comment that links to the original project.
*   We don't make any guarantees of API stability or back/forward compatibility between different versions of the library.
    We routinely make breaking changes or delete code that we're no longer using.



## Release process

Our process is [inspired by the Hypothesis library](https://hypothesis.works/articles/continuous-releases/).

1.  When you open a pull request, include a release note named `RELEASE.md` in the root of the repo:

    ```md
    RELEASE_TYPE: major|minor|patch

    Updating the widget wrangler to reverse the polarity of the neutron flow.
    ```

2.  When the pull request is merged, our CI system merges the release note into [the changelog](./CHANGELOG.md) and cuts a new release.
    This release is uploaded to our public Sonatype repository.

3.  Our CI system then opens pull requests on our downstream repos, updating them to use the newly released version.

This means that new changes are deployed quickly and consistently across the platform.
If we've made breaking changes that need more changes in the downstream repo, that happens immediately (usually by the author of the original patch).

### Maven Central (Sonatype)

When a new release is merged into the main branch, all packages are automatically published to our [public Sonatype repository](https://central.sonatype.com/namespace/org.wellcomecollection).
This is done through a [GitHub action](.github/workflows/release.yml).

Packages are published into the `org.wellcomecollection` namespace, ownership of which had to be proven by adding a TXT record to the wellcomecollection.org Route 53 hosted zone. 

The credentials to our Sonatype account and a base64 encoded GPG key (and corresponding passphrase) are stored in AWS Secrets Manager in the platform account.
