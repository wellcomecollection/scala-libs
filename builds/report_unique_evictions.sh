echo "suspected binary incompatible evictions across all projects"
cat .reports/evicted_* | grep -E '^\[warn\]\s+\*' | sort | uniq