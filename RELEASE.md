RELEASE_TYPE: minor

This replaces the assert() helpers with custom Scalatest matchers in JsonAssertions and TimeAssertions, allowing you to write more Scalatest-like code.

e.g.

```diff
-assertJsonStringsAreEqual(json1, json2)
+json1 shouldBe equivalentJsonTo(json2)
```

```diff
-assertRecent(t)
+t shouldBe recent()
```
