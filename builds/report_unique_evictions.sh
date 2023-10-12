# Intended to be run after producing eviction reports with report_sbt_evictions.sh
echo "# Suspected binary incompatible evictions across all projects (summary)"
cat .reports/evicted_* | grep -E '^\[warn\]\s+\*' | sed 's/.*\*/*/' | sort | uniq
echo ""
echo "See individual _evictions_ stages for more detail"
