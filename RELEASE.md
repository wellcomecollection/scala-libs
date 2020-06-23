RELEASE_TYPE: major

### Libraries affected

`storage`

### Description

This removes the type `HybridStoreEntry` meaning that for some types `Id` and `Data`, instead of `HybridStore` being a

```
Store[Id, HybridStoreEntry[Data, Metadata]
```

it is now a

```
Store[Id, Data]
```

This is an improvement for any code that might use a hybrid store, as previously anywhere that wanted to use it had to encode the fact that a hybrid store was being used, even though this is just an implementation detail and should not really be part of the interface (we generally should only care that some `Id` maps to some `Data`, and should abstract away the internal details of the storage).

This means that we can remove workarounds such as
[here](https://github.com/wellcomecollection/catalogue/blob/2013710c9bd54fa8644f3b2c3bb618af714a2554/common/big_messaging/src/main/scala/uk/ac/wellcome/bigmessaging/VHS.scala#L50-L77).

Note that the ability to store metadata in the IndexedStore (i.e. DynamoDB) is now not possible: this is only used in the Miro VHS, which does not use the current `HybridStore` interface anyway, being created with an older schema for the data. Storing metadata is such a way should now be considered deprecated.
