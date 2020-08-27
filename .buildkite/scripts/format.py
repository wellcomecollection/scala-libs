#!/usr/bin/env python
# -*- encoding: utf-8

import sys

from commands import git, sbt
from git_utils import (
    get_changed_paths,
    remote_default_branch,
    local_current_head,
    get_sha1_for_tag,
    remote_default_head
)
from provider import current_branch, is_default_branch, repo
from release import check_release_file, parse_release_file, has_release


def autoformat():
    local_head = local_current_head()

    if is_default_branch():
        latest_sha = get_sha1_for_tag("latest")
        commit_range = f"{latest_sha}..{local_head}"
    else:
        remote_head = remote_default_head()
        commit_range = f"{remote_head}..{local_head}"

    print(f"Working in branch: {current_branch()}")
    print(f"On default branch: {is_default_branch()}")
    print(f"Commit range: {commit_range}")

    sbt('scalafmt')

    check_release_file(commit_range)

    # If there are any changes, push to GitHub immediately and fail the
    # build.  This will abort the remaining jobs, and trigger a new build
    # with the reformatted code.
    if get_changed_paths():
        print("*** There were changes from formatting, creating a commit")

        git("config", "user.name", "Buildkite on behalf of Wellcome Collection")
        git("config", "user.email", "wellcomedigitalplatform@wellcome.ac.uk")
        git("remote", "add", "ssh-origin", repo(), exit_on_error=False)

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Buildkite makes.
        git("fetch", "ssh-origin")
        git("checkout", "--track", f"ssh-origin/{current_branch()}")

        git("add", "--verbose", "--update")
        git("commit", "-m", "Apply auto-formatting rules")
        git("push", "ssh-origin", f"HEAD:{current_branch()}")

        # We exit here to fail the build, so Buildkite will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print("*** There were no changes from auto-formatting")


if __name__ == '__main__':
    autoformat()
