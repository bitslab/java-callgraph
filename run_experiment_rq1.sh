#!/bin/bash

# exit if anything throws a bad exit code
set -e

# SET JCG_HOME based on the directory where this script resides
JCG_HOME="$(pwd)/$( dirname -- "$0"; )";

cd $JCG_HOME || exit

mkdir -p artifacts/experiment

for PROJECT in mph-table convex jflex rpki-commons
do
	FILE=artifacts/experiment/rq1_$PROJECT.csv

	# run experiment
	echo Running RQ1 for project $PROJECT with output going to $FILE
	java -cp target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar edu.uic.bitslab.callgraph.Comparison -p $PROJECT -o $FILE

	# check that the expected file exists
	if [ ! -f $FILE ]; then
	  echo "Experiment did not produce the expected output file"
		exit
	fi
done

python rq1_data.py

