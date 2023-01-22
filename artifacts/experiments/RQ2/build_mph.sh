java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-1000

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/mph-table/mph-table-1.0.6-SNAPSHOT.jar -t ./artifacts/output/mph-table/mph-table-1.0.6-SNAPSHOT.jar -o mph-table_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/mph-table-10/mph-table-1.0.6-SNAPSHOT.jar -t ./artifacts/output/mph-table-10/mph-table-1.0.6-SNAPSHOT.jar -o mph-table-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/mph-table-50/mph-table-1.0.6-SNAPSHOT.jar -t ./artifacts/output/mph-table-50/mph-table-1.0.6-SNAPSHOT.jar -o mph-table-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/mph-table-500/mph-table-1.0.6-SNAPSHOT.jar -t ./artifacts/output/mph-table-500/mph-table-1.0.6-SNAPSHOT.jar -o mph-table-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -j ./artifacts/output/mph-table-1000/mph-table-1.0.6-SNAPSHOT.jar -t ./artifacts/output/mph-table-1000/mph-table-1.0.6-SNAPSHOT.jar -o mph-table-1000_graph