#!/bin/bash

# SET JCG_HOME based on the directory wehere this script resides
JCG_HOME="$(pwd)/$( dirname -- "$0"; )";

declare -A mainjar
mainjar[convex]=convex-core-0.7.1-jar-with-dependencies.jar
mainjar[jflex]=jflex-1.8.2-jar-with-dependencies.jar
mainjar[mph-table]=mph-table-1.0.6-SNAPSHOT-jar-with-dependencies.jar

declare -A testjar
testjar[convex]=convex-core-0.7.1-tests.jar
testjar[jflex]=jflex-1.8.2-tests.jar
testjar[mph-table]=mph-table-1.0.6-SNAPSHOT-tests.jar


cd $JCG_HOME || exit

for type in original fixed
do
  for project in convex jflex mph-table
  do
    echo $type for $project
    
    # clean project
    rm -rf $project
    
    # clean output
    rm -rf output
    mkdir output
    
    # move config files
    cp artifacts/configs/$project/$project.$type.yaml artifacts/configs/$project/$project.yaml
    cp artifacts/configs/$project/$project.$type.patch artifacts/configs/$project/$project.patch
    
    # git project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c $project
    
    # build project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/${mainjar[$project]} -t ./artifacts/output/${testjar[$project]} -o $project-$type

    # test project
    java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar test -c $project -f $project-$type

    # copy output
    rm -rf output-$project-$type
    mv output output-$project-$type

		cd output-$project-$type
    ../buildsvg.sh
		cd ..
  done
done


