mkdir -p .reports
REPORT_FILE=".reports/evicted_${1}"
./builds/run_sbt_task_in_docker.sh "project $1" "evicted" | grep '^\[warn\]' | tee $REPORT_FILE
WARNING_COUNT=$(cat $REPORT_FILE | grep -E '^\[warn\]\s+\*' | wc -l )
if [ "$WARNING_COUNT" != "0" ]
then
  echo "found $WARNING_COUNT suspected binary incompatible eviction(s)"
  exit 2
fi
