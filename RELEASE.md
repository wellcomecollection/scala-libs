RELEASE_TYPE: minor

Remove some unneeded Materializer instances:

In [Akka 2.6](https://doc.akka.io/docs/akka/current/project/migration-guide-2.5.x-2.6.x.html#materializer-changes), you don't need to supply an implicit Materializer if there's an implicit ActorSystem:

> A default materializer is now provided out of the box. For the Java API just pass system when running streams, for Scala an implicit materializer is provided if there is an implicit ActorSystem available. This avoids leaking materializers and simplifies most stream use cases somewhat.

The `withMaterializer(actorSystem: ActorSystem)` method has been removed from the `Akka` fixture; you can remove it anywhere it's used and rely on the implicit materializer.
