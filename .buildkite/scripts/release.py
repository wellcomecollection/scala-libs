#!/usr/bin/env python
# -*- encoding: utf-8

import datetime as dt
import os
import re
import sys

from commands import git
from git_utils import (
    get_changed_paths,
    get_all_tags,
    remote_default_branch,
    local_current_head,
    get_sha1_for_tag,
    remote_default_head,
    has_source_changes
)
from provider import current_branch, is_default_branch, repo


ROOT = git('rev-parse', '--show-toplevel')

BUILD_SBT = os.path.join(ROOT, 'build.sbt')

RELEASE_FILE = os.path.join(ROOT, 'RELEASE.md')
RELEASE_TYPE = re.compile(r"^RELEASE_TYPE: +(major|minor|patch)")

MAJOR = 'major'
MINOR = 'minor'
PATCH = 'patch'

VALID_RELEASE_TYPES = (MAJOR, MINOR, PATCH)

CHANGELOG_HEADER = re.compile(r"^## v\d+\.\d+\.\d+ - \d\d\d\d-\d\d-\d\d$")
CHANGELOG_FILE = os.path.join(ROOT, 'CHANGELOG.md')


def changelog():
    with open(CHANGELOG_FILE) as i:
        return i.read()


def new_version(release_type):
    version = latest_version()
    version_info = [int(i) for i in version.lstrip('v').split('.')]

    new_version = list(version_info)
    bump = VALID_RELEASE_TYPES.index(release_type)
    new_version[bump] += 1
    for i in range(bump + 1, len(new_version)):
        new_version[i] = 0
    new_version = tuple(new_version)
    return 'v' + '.'.join(map(str, new_version))


def update_changelog_and_version():
    contents = changelog()
    assert '\r' not in contents
    lines = contents.split('\n')
    assert contents == '\n'.join(lines)
    for i, l in enumerate(lines):
        if CHANGELOG_HEADER.match(l):
            beginning = '\n'.join(lines[:i])
            rest = '\n'.join(lines[i:])
            assert '\n'.join((beginning, rest)) == contents
            break

    release_type, release_contents = parse_release_file()

    new_version_string = new_version(release_type)

    print('New version: %s' % new_version_string)

    now = dt.datetime.utcnow()

    date = max([
        d.strftime('%Y-%m-%d') for d in (now, now + dt.timedelta(hours=1))
    ])

    heading_for_new_version = '## ' + ' - '.join((new_version_string, date))

    new_changelog_parts = [
        beginning.strip(),
        '',
        heading_for_new_version,
        '',
        release_contents,
        '',
        rest
    ]

    with open(CHANGELOG_FILE, 'w') as o:
        o.write('\n'.join(new_changelog_parts))

    # Update the version specified in build.sbt.  We're looking to replace
    # a line of the form:
    #
    #       version := "x.y.z"
    #
    lines = list(open(BUILD_SBT))
    for idx, l in enumerate(lines):
        if l.startswith('val projectVersion = '):
            lines[idx] = 'val projectVersion = "%s"\n' % new_version_string.strip('v')
            break
    else:  # no break
        raise RuntimeError('Never updated version in build.sbt?')

    with open(BUILD_SBT, 'w') as f:
        f.write(''.join(lines))

    return release_type


def update_for_pending_release():
    release_type = update_changelog_and_version()

    git('rm', RELEASE_FILE)
    git('add', CHANGELOG_FILE)
    git('add', BUILD_SBT)

    git(
        'commit',
        '-m', 'Bump version to %s and update changelog\n\n[skip ci]' % (
            new_version(release_type))
    )
    git('tag', new_version(release_type))


def has_release():
    """
    Returns True if there is a release file, False if not.
    """
    return os.path.exists(RELEASE_FILE)


def latest_version():
    """
    Returns the latest version, as specified by the Git tags.
    """
    versions = []

    for t in get_all_tags():
        assert t == t.strip()
        parts = t.split('.')
        assert len(parts) == 3, t
        parts[0] = parts[0].lstrip('v')
        v = tuple(map(int, parts))

        versions.append((v, t))

    _, latest = max(versions)

    assert latest in get_all_tags()
    return latest


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


def release():
    local_head = local_current_head()

    if is_default_branch():
        latest_sha = get_sha1_for_tag(latest_version())
        commit_range = f"{latest_sha}..{local_head}"
    else:
        remote_head = remote_default_head()
        commit_range = f"{remote_head}..{local_head}"

    print(f"Working in branch: {current_branch()}")
    print(f"On default branch: {is_default_branch()}")
    print(f"Commit range: {commit_range}")

    if not is_default_branch():
        print('Trying to release while not on main?')
        sys.exit(1)

    if has_release():
        print('Updating changelog and version')

        update_for_pending_release()

        print('Attempting a release.')

        git("config", "user.name", "Buildkite on behalf of Wellcome Collection")
        git("config", "user.email", "wellcomedigitalplatform@wellcome.ac.uk")
        git("remote", "add", "ssh-origin", repo(), exit_on_error=False)

        git('push', 'ssh-origin', 'HEAD:main')
        git('push', 'ssh-origin', '--tag')
    else:
        print("No release detected, exit gracefully.")
        sys.exit(0)


if __name__ == '__main__':
    release()
