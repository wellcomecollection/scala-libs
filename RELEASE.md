RELEASE_TYPE: patch

Fix a bug in the LockingService where locks weren't being released correctly if there was a partial locking failure.

e.g. if you tried to lock (A, B, C), successfully locked A and B but failed to lock C, then the locks for A and B wouldn't be released.
Now they get released correctly.
