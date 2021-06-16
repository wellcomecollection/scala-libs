#!/usr/bin/env python
"""
Whenever CI releases a new version of scala-libs, this script will create
a pull request in downstream repos that bumps to the new version.

This ensures that our repos don't fall behind, and any breaking changes can
be resolved promptly.

This script is quite light on error handling -- because it runs almost as
soon as the release becomes available, it seems unlikely that it would
conflict with a manually created PR/branch.
"""

import contextlib
import os
import re
import shutil
import tempfile

import boto3
import httpx

from commands import git
from release import latest_version


DOWNSTREAM_REPOS = ("catalogue-api", "catalogue-pipeline", "storage-service")


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
                version_string = new_version.strip('v')
                out_file.write(f'  val defaultVersion = "{version_string}"  // This is automatically bumped by the scala-libs release process, do not edit this line manually\n')
            else:
                out_file.write(line)


def get_github_api_key():
    session = boto3.Session()
    secrets_client = session.client("secretsmanager")

    secret_value = secrets_client.get_secret_value(SecretId="builds/github_wecobot/scala_libs_pr_bumps")

    return secret_value["SecretString"]


def get_changelog_entry():
    with open("CHANGELOG.md") as f:
        changelog = f.read()

    # This gets us something like:
    #
    #     '## v26.18.0 - 2021-06-16\n\nAdd an HTTP typesafe builder for the SierraOauthHttpClient.\n\n'
    #
    last_entry = changelog.split("## ")[1]

    # Then remove that first header
    lines = last_entry.splitlines()[1:]

    return "\n".join(lines).strip()


def get_last_merged_pr_number():
    for line in git("log", "--oneline").splitlines():
        m = re.match(r"^[0-9a-f]{8} Merge pull request #(?P<pr_number>\d+)", line)

        if m is not None:
            return m.group("pr_number")


def create_downstream_pull_requests(new_version):
    api_key = get_github_api_key()

    client = httpx.Client(auth=("weco-bot", api_key))

    changelog = get_changelog_entry()

    pr_number = get_last_merged_pr_number()

    pr_body = "\n".join([
        "Changelog entry:\n"
    ] + [f"> {line}" for line in changelog.splitlines()] + [
        f"\nSee wellcomecollection/scala-libs#{pr_number}"
    ])

    for repo in DOWNSTREAM_REPOS:
        with cloned_repo(f"git@github.com:wellcomecollection/{repo}.git"):
            update_scala_libs_version(new_version)

            branch_name = f"bump-scala-libs-to-{new_version}"

            git("config", "--local", "user.email", "wellcomedigitalplatform@wellcome.ac.uk")
            git("config", "--local", "user.name", "BuildKite on behalf of Wellcome Collection")

            git("checkout", "-b", branch_name)
            git("add", "project/Dependencies.scala")
            git("commit", "-m", f"Bump scala-libs to {new_version}")
            git("push", "origin", branch_name)

            r = client.post(
                f"https://api.github.com/repos/wellcomecollection/{repo}/pulls",
                headers={"Accept": "application/vnd.github.v3+json"},
                json={
                    "head": branch_name,
                    "base": "main",
                    "title": f"Bump scala-libs to {new_version}",
                    "maintainer_can_modify": True,
                    "body": pr_body,
                }
            )

            try:
                r.raise_for_status()
                new_pr_number = r.json()["number"]
            except Exception:
                print(r.json())
                raise

            r = client.post(
                f"https://api.github.com/repos/wellcomecollection/{repo}/pulls/{new_pr_number}/requested_reviewers",
                headers={"Accept": "application/vnd.github.v3+json"},
                json={"reviewers": ["scala-devs"]}
            )

            r.raise_for_status()


if __name__ == '__main__':
    create_downstream_pull_requests(
        new_version=latest_version()
    )
