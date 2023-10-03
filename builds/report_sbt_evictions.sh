mkdir -p .reports
./builds/run_sbt_task_in_docker.sh "project $1" "evicted" | grep '^\[warn\]' | tee .reports/evicted
WARNING_COUNT=$(cat .reports/evicted | sed '/^$/d' | wc -l )
if [ $WARNING_COUNT != 0 ]
  exit 2
fi