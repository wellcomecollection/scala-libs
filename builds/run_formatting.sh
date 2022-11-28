#!/usr/bin/env bash

set -o errexit
set -o nounset

./builds/run_sbt_task_in_docker.sh "scalafmt"
