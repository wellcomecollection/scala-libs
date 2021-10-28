RELEASE_TYPE: patch

This changes how we log HTTP requests.
Our current logs are fairly noisy, and split across three lines:

> 08:42:26.038 [main-actor-system-akka.actor.default-dispatcher-26] INFO akka.actor.ActorSystemImpl - SearchApi/b15d78e0-af82-4630-aaa8-49000ee3bb3d: Response for
> Request : HttpRequest(HttpMethod(GET),http://localhost:8888/works/qqqmwej7?include=identifiers,images,items,subjects,genres,contributors,production,notes,parts,partOf,precededBy,succeededBy,languages,holdings,List(Timeout-Access: <function1>, Host: localhost:8888, Connection: close, Accept-Encoding: gzip, elastic-apm-traceparent: 00-a8471e6468afac466583488f55f70f62-e070a1f1322fb613-01, traceparent: 00-a8471e6468afac466583488f55f70f62-e070a1f1322fb613-01, tracestate: es=s:1, User-Agent: Amazon CloudFront, Via: 1.1 5375075eb87a09bb90c63fb4a8d064f4.cloudfront.net (CloudFront), X-Amz-Cf-Id: baxKLJQNmWTDwysAsqsSRZAYYTwFlfB5k95dJHLGlBN5NrR1SBTE9A==, X-Amzn-Trace-Id: Root=1-617a6272-56a62abd10473a27311cfa64, X-Forwarded-For: 34.240.107.39, 64.252.133.110, X-Forwarded-Port: 443, X-Forwarded-Proto: https, x-amzn-apigateway-api-id: 4fd60k0q27),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.0))
> Response: Complete(HttpResponse(200 OK,List(),HttpEntity.Strict(application/json,3256 bytes total),HttpProtocol(HTTP/1.1)))

This is because we were using the akka-http DebuggingDirectives, which log everything.

Now we construct our own log lines, which are significantly more compact:

> 08:42:26.038 [main-actor-system-akka.actor.default-dispatcher-26] INFO akka.actor.ActorSystemImpl - SearchApi/b15d78e0-af82-4630-aaa8-49000ee3bb3d - Request: GET /works/qqqmwej7?include=identifiers,images,items,subjects,genres,contributors,production,notes,parts,partOf,precededBy,succeededBy,languages,holdings / Response: HTTP 200 OK; HttpEntity.Strict(application/json,3256 bytes total)

This has a number of benefits:

*   The request/response are logged on the same line, so it'll be easier to pair them up in the logging cluster.
*   There's less noise from headers we don't care about.
*   We have to explicitly label the headers we want to log, so we aren't going to inadvertently log sensitive information in headers (for example, authentication tokens).
