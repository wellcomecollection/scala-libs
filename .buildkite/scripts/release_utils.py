#!/usr/bin/env python
# -*- encoding: utf-8

import os
import re

from commands import git
from git_utils import has_source_changes

ROOT = git('rev-parse', '--show-toplevel')

RELEASE_FILE = os.path.join(ROOT, 'RELEASE.md')
RELEASE_TYPE = re.compile(r"^RELEASE_TYPE: +(major|minor|patch)")

MAJOR = 'major'
MINOR = 'minor'
PATCH = 'patch'

VALID_RELEASE_TYPES = (MAJOR, MINOR, PATCH)


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
