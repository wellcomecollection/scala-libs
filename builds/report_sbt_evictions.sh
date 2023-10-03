mkdir -p .reports
./builds/run_sbt_task_in_docker.sh "project $1" "evicted" | grep '^\[warn\]' | tee .reports/evicted
WARNING_COUNT=$(cat .reports/evicted | grep -E '^\[warn\]\s+\*' | wc -l )
if [ "$WARNING_COUNT" != "0" ]
then
  echo "found $WARNING_COUNT suspected binary incompatible evictions"
  exit 2
fi