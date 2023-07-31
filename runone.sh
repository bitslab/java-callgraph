#!/bin/bash

# exit if anything throws a bad exit code
set -e

# SET JCG_HOME based on the directory where this script resides
JCG_HOME="$(pwd)/$( dirname -- "$0"; )";

cd $JCG_HOME || exit

mkdir -p serializedGraphs

# check $1 to be sure it is at least 1 character and only contains alpha, number, _, and -.
if [[ $# -ne 1 || ! $1 =~ ^[A-Za-z0-9_\-]+$ ]]; then
  echo "Provide a project name (alphanumeric with _ and - allowed).";
else
  projectName=$1

  # clean project
  rm -rf "$projectName"

  # clean output
  rm -rf output
  mkdir output

	# add results (if not exists)
  mkdir -p "artifacts/results/$projectName"

  # git project
  java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c $projectName

  # build project
  java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c $projectName -o serializedGraphs/$projectName

  cp "serializedGraphs/$projectName" $projectName"_graph"

  # test project
  java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar test -c $projectName -f serializedGraphs/$projectName

  # copy output
  rm -rf output-$projectName
  mv output output-$projectName

  cd output-$projectName || exit
  ../buildsvg.sh
  cd ..
fi
