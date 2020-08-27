#!/usr/bin/env python
# -*- encoding: utf-8

import datetime as dt
import os
import re
import shutil
import subprocess
import sys


ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


def git(*args):
    """
    Run a Git command and check it completes successfully.
    """
    subprocess.check_call(('git',) + args)


def sbt(*args):
    """
    Run an sbt command and check it completes successfully.
    """
    subprocess.check_call(('sbt',) + args)


def tags():
    """
    Returns a list of all tags in the repo.
    """
    git('fetch', '--tags')
    result = subprocess.check_output(['git', 'tag']).decode('ascii').strip()
    all_tags = result.split('\n')

    assert len(set(all_tags)) == len(all_tags)

    return set(all_tags)


def latest_version():
    """
    Returns the latest version, as specified by the Git tags.
    """
    versions = []

    for t in tags():
        assert t == t.strip()
        parts = t.split('.')
        assert len(parts) == 3, t
        parts[0] = parts[0].lstrip('v')
        v = tuple(map(int, parts))

        versions.append((v, t))

    _, latest = max(versions)

    assert latest in tags()
    return latest


def modified_files():
    """
    Returns a list of all files which have been modified between now
    and the latest release.
    """
    files = set()
    for command in [
        ['git', 'diff', '--name-only', '--diff-filter=d',
            latest_version(), 'HEAD'],
        ['git', 'diff', '--name-only']
    ]:
        diff_output = subprocess.check_output(command).decode('ascii')
        for l in diff_output.split('\n'):
            filepath = l.strip()
            if filepath:
                assert os.path.exists(filepath)
                files.add(filepath)
    return files


def has_source_changes():
    """
    Returns True if there are source changes since the previous release,
    False if not.
    """
    changed_files = [
        f for f in modified_files() if f.strip().endswith(('.sbt', '.scala'))
    ]
    return len(changed_files) != 0


RELEASE_FILE = os.path.join(ROOT, 'RELEASE.md')


def has_release():
    """
    Returns True if there is a release file, False if not.
    """
    return os.path.exists(RELEASE_FILE)


RELEASE_TYPE = re.compile(r"^RELEASE_TYPE: +(major|minor|patch)")

MAJOR = 'major'
MINOR = 'minor'
PATCH = 'patch'

VALID_RELEASE_TYPES = (MAJOR, MINOR, PATCH)


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


def check_release_file():
    if has_source_changes():
        if not has_release():
            print(
                'There are source changes but no RELEASE.md. Please create '
                'one to describe your changes.'
            )
            sys.exit(1)
        parse_release_file()
    else:
        print('No source changes detected. No requirement for RELEASE.md')


def configure_secrets():
    subprocess.check_call(['unzip', 'secrets.zip'])

    os.makedirs(os.path.join(os.environ['HOME'], '.aws'))
    shutil.copyfile(
        src='awscredentials',
        dst=os.path.join(os.environ['HOME'], '.aws', 'credentials')
    )

    subprocess.check_call(['chmod', '600', 'id_rsa'])
    git('config', 'core.sshCommand', 'ssh -i id_rsa')

    git('config', 'user.name', 'Travis CI on behalf of Wellcome')
    git('config', 'user.email', 'wellcomedigitalplatform@wellcome.ac.uk')

    print('SSH public key:')
    subprocess.check_call(['ssh-keygen', '-y', '-f', 'id_rsa'])


def branch_name():
    """Return the name of the branch under test."""
    # See https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
    if os.environ['TRAVIS_PULL_REQUEST'] == 'false':
        return os.environ['TRAVIS_BRANCH']
    else:
        return os.environ['TRAVIS_PULL_REQUEST_BRANCH']


def autoformat():
    sbt('scalafmt')

    check_release_file()

    # If there are any changes, push to GitHub immediately and fail the
    # build.  This will abort the remaining jobs, and trigger a new build
    # with the reformatted code.
    if subprocess.call(['git', 'diff', '--exit-code']):
        print('There were changes from formatting, creating a commit')

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Travis makes.
        git('fetch', 'ssh-origin')
        git('checkout', branch_name())

        git('add', '--verbose', '--update')
        git('commit', '-m', 'Apply auto-formatting rules')
        git('push', 'ssh-origin', 'HEAD:%s' % branch_name())

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print('There were no changes from auto-formatting')


if __name__ == '__main__':
    autoformat()
