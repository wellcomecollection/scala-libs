RELEASE_TYPE: minor

Remove the `refreshInterval` setting from `IndexConfig`.

We originally used this to pause refreshes when doing reindexes in the catalogue pipeline,by setting refresh_interval=-1 to disable refreshes until a reindex was done.  This was intended to improve reindex performance.

In practice, it caused confusion, because the final index would appear to have 0 documents for no obvious reason.

We don't use this setting anywhere else, and we can run a performant reindex without this setting, so we're removing it.