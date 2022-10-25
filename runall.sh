#!/bin/bash

# SET JCG_HOME based on the directory where this script resides
JCG_HOME="$(pwd)/$( dirname -- "$0"; )";

cd $JCG_HOME || exit

mkdir -p serializedGraphs


for type in original fixed
do
  for project in convex jflex mph-table # JQF rpki-commons
  do
    echo $type for $project

    if [[ "$type" == "original" ]]
    then
      projectName=$project
    else
      projectName=$project-$type
    fi

    ./runone.sh $projectName
  done
done

