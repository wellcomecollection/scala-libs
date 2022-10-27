RELEASE_TYPE: patch

Fix an issue where the `AzureTransfer` class might try to overwrite a blob in Azure if it got a transient error while trying to retrieve the blob in the destination.  Also log when objects/blobs are overwritten because the destination object can't be retrieved and it's not a 404 Not Found.