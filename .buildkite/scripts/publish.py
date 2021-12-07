import sys

from commands import git, run_build_script
from git_utils import remote_default_branch
from release import has_release

def publish(project_name):
    if has_release():
        print(f"Release detected, publishing {project_name}.")
        git("pull", "origin", remote_default_branch())
        run_build_script("run_sbt_task_in_docker.sh", f"project {project_name}", "publish")
    else:
        print("No release detected, exit gracefully.")
        sys.exit(0)


# This script takes environment variables as the "command" step
# when used with the buildkite docker plugin incorrectly parses
# spaces as newlines preventing passing args to this script!
if __name__ == '__main__':
    project = sys.argv[1]

    publish(project)
