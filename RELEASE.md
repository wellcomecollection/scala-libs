RELEASE_TYPE: major

### Libraries affected

`storage`

### Description

`RetryableGet` has been replaced in favour of `RetryableReadable`. 

A new trait `Updatable` has been made available and is used in `Tags`.

```scala
trait Updatable[Ident, T] {
  type UpdateEither = Either[UpdateError, Identified[Ident, T]]
  type UpdateFunction = T => Either[UpdateFunctionError, T]

  def update(id: Ident)(updateFunction: UpdateFunction): UpdateEither
}
```

`Tags` now meets the interfaces of  to `RetryableReadable` and `Updatable`.

Consumers will need to update code where `update` and `get` have been used on `S3Tags`.
