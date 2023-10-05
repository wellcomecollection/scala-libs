echo "# Suspected binary incompatible evictions across all projects (distinct)"
cat .reports/evicted_* | grep -E '^\[warn\]\s+\*' | sed 's/.*\*/*/'  |sort | uniq
echo '```'
echo '```'
