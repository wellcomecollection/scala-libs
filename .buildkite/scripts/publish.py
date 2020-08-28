# -*- encoding: utf-8

import os

from commands import sbt, git
from git_utils import remote_default_branch


def publish(project_name):
    git("pull", "origin", remote_default_branch())
    sbt(f"project {project_name}", "publish")


# This script takes environment variables as the "command" step
# when used with the buildkite docker plugin incorrectly parses
# spaces as newlines preventing passing args to this script!
if __name__ == '__main__':
    project = os.environ["PROJECT"]

    publish(project)
