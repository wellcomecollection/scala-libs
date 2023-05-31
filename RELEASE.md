RELEASE_TYPE: minor

Restore the AWS CRT libraries, because their absence is causing the bag replicator to fail when transferring files >5GB in size.