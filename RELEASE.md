RELEASE_TYPE: major

This release removes all the typesafe config for AWS-related config, in particular:

- `aws.region` -- use the `AWS_REGION` environment variable instead
-	`aws.key` -- use the `AWS_ACCESS_KEY_ID` environment variable instead
-	`aws.secret` -- use the `AWS_SECRET_ACCESS_KEY` environment variable instead
- `aws.max-connections` -- this was never used in practice
- `aws.endpoint` -- there is no alternative to this for now
