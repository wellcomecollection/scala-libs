RELEASE_TYPE: minor

Allow specifying global options for AWS client config, e.g. you can configure a default AWS region with

```
aws.region=${?aws_region
```

rather than having to specify it for every namespace/AWS service that you're using.