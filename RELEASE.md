RELEASE_TYPE: minor

This standardises the use of context URLs with a new trait:

```scala
trait HasContextUrl {
  def contextUrl: URL
}
```
