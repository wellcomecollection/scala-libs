#!/usr/bin/env python

import contextlib
import os
import shutil
import tempfile

from commands import git


@contextlib.contextmanager
def working_directory(path):
    """
    Changes the working directory to the given path, then returns to the
    original directory when done.
    """
    prev_cwd = os.getcwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(prev_cwd)


@contextlib.contextmanager
def cloned_repo(git_url):
    """
    Clones the repository and changes the working directory to the cloned
    repo.  Cleans up the clone when it's done.
    """
    repo_dir = tempfile.mkdtemp()

    git("clone", git_url, repo_dir)

    try:
        with working_directory(repo_dir):
            yield
    finally:
        shutil.rmtree(repo_dir)


def update_scala_libs_version(new_version):
    old_lines = list(open("project/Dependencies.scala"))

    with open("project/Dependencies.scala", "w") as out_file:
        for line in old_lines:
            if line.startswith("  val defaultVersion"):
                out_file.write(f'  val defaultVersion = "{new_version}"\n')
            else:
                out_file.write(line)


if __name__ == '__main__':
    new_version = "v26.18.0"

    for repo in ("catalogue-api", "catalogue-pipeline", "storage-service"):
        with cloned_repo(f"git@github.com:wellcomecollection/{repo}.git"):
            update_scala_libs_version(new_version)

            branch_name = f"bump-scala-libs-to-{new_version}"

            git("config", "--local", "user.email", "digital@wellcomecollection.org")
            git("config", "--local", "user.name", "BuildKite on behalf of Wellcome Collection")

            git("checkout", "-b", branch_name)
            git("add", "project/Dependencies.scala")
            git("commit", "-m", f"Bump scala-libs to {new_version}")
            git("push", "origin", branch_name)

        print(os.listdir(tmp_dir))
