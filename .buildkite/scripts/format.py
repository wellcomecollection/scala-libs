#!/usr/bin/env python
# -*- encoding: utf-8

import os
import re
import subprocess
import sys

from commands import git, sbt
from git_utils import get_changed_paths, remote_default_branch, local_current_head, get_sha1_for_tag, remote_default_head
from provider import current_branch, is_default_branch, repo

ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

RELEASE_FILE = os.path.join(ROOT, 'RELEASE.md')

RELEASE_TYPE = re.compile(r"^RELEASE_TYPE: +(major|minor|patch)")

MAJOR = 'major'
MINOR = 'minor'
PATCH = 'patch'

VALID_RELEASE_TYPES = (MAJOR, MINOR, PATCH)


def has_source_changes(commit_range):
    """
    Returns True if there are source changes since the previous release,
    False if not.
    """
    changed_files = [
        f for f in get_changed_paths(commit_range) if f.strip().endswith(('.sbt', '.scala'))
    ]
    return len(changed_files) != 0


def has_release():
    """
    Returns True if there is a release file, False if not.
    """
    return os.path.exists(RELEASE_FILE)


def parse_release_file():
    """
    Parses the release file, returning a tuple (release_type, release_contents)
    """
    with open(RELEASE_FILE) as i:
        release_contents = i.read()

    release_lines = release_contents.split('\n')

    m = RELEASE_TYPE.match(release_lines[0])
    if m is not None:
        release_type = m.group(1)
        if release_type not in VALID_RELEASE_TYPES:
            print('Unrecognised release type %r' % (release_type,))
            sys.exit(1)
        del release_lines[0]
        release_contents = '\n'.join(release_lines).strip()
    else:
        print(
            'RELEASE.md does not start by specifying release type. The first '
            'line of the file should be RELEASE_TYPE: followed by one of '
            'major, minor, or patch, to specify the type of release that '
            'this is (i.e. which version number to increment). Instead the '
            'first line was %r' % (release_lines[0],)
        )
        sys.exit(1)

    return release_type, release_contents


def check_release_file(commit_range):
    if has_source_changes(commit_range):
        if not has_release():
            print(
                'There are source changes but no RELEASE.md. Please create '
                'one to describe your changes.'
            )
            sys.exit(1)

        print('Source changes detected (RELEASE.md is present).')
        parse_release_file()
    else:
        print('No source changes detected (RELEASE.md not required).')


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
