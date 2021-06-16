#!/usr/bin/env python

import os
import tempfile

from commands import git


if __name__ == '__main__':
    new_version = "v26.18.0"

    for repo in ("catalogue-api", "pipeline", "storage-service"):
        tmp_dir = tempfile.mkdtemp()

        git("clone", f"git@github.com:wellcomecollection/{repo}.git", tmp_dir)
        print(os.listdir(tmp_dir))