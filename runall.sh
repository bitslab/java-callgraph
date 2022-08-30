#!/bin/bash

# SET JCG_HOME based on the directory where this script resides
JCG_HOME="$(pwd)/$( dirname -- "$0"; )";

declare -A mainjar
mainjar[convex]=convex-core-0.7.1-jar-with-dependencies.jar
mainjar[jflex]=jflex-1.8.2-jar-with-dependencies.jar
mainjar[mph-table]=mph-table-1.0.6-SNAPSHOT-jar-with-dependencies.jar
mainjar[JQF]=jqf-fuzz-1.9-jar-with-dependencies.jar
mainjar[rpki-commons]=rpki-commons-DEV.jar


declare -A testjar
testjar[convex]=convex-core-0.7.1-tests.jar
testjar[jflex]=jflex-1.8.2-tests.jar
testjar[mph-table]=mph-table-1.0.6-SNAPSHOT-tests.jar
testjar[JQF]=jqf-fuzz-1.9-tests.jar
testjar[rpki-commons]=rpki-commons-DEV-tests.jar


cd $JCG_HOME || exit

mkdir -p serializedGraphs


for type in original fixed
do
  for project in convex jflex mph-table JQF rpki-commons
  do
    echo $type for $project

    if [[ "$type" == "original" ]]
    then
      projectName=$project
    else
      projectName=$project-$type
    fi

    # clean project
    rm -rf $projectName
    
    # clean output
    rm -rf output
    mkdir output
    
    # git project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c $projectName

    # build project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/${mainjar[$project]} -t ./artifacts/output/${testjar[$project]} -o serializedGraphs/$projectName

    # test project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar test -c $projectName -f serializedGraphs/$projectName

    # copy output
    rm -rf output-$projectName
    mv output output-$projectName

		cd output-$projectName || exit
    ../buildsvg.sh
		cd ..
  done
done


