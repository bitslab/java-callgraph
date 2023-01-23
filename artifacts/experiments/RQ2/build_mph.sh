java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-1000

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table -o mph-table_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-10 -o mph-table-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-50 -o mph-table-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-500 -o mph-table-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-1000 -o mph-table-1000_graph
