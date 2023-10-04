echo "# Distinct suspected binary incompatible evictions across all projects"
echo '```'
cat .reports/evicted_* | grep -E '^\[warn\]\s+\*' | sort | uniq
echo '```'
