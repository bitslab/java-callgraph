GEN_STATS="artifacts/experiments/RQ2/generateStats.py"
GEN_RESULTS="artifacts/experiments/RQ2/generateResultsV2.py"
for PROJECT in convex jflex mph-table rpki-commons
do
  echo Generating statistics for $PROJECT
  python3 $GEN_STATS $PROJECT
done
echo Generating results for projects
python3 $GEN_RESULTS
