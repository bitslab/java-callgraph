java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-1000
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-1000
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-1000
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-10
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-50
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-500
java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-1000

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-10 -o mph-table-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-50 -o mph-table-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-500 -o mph-table-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-1000 -o mph-table-1000_graph

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-10 -o convex-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-50 -o convex-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-500 -o convex-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-1000 -o convex-1000_graph

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-10 -o jflex-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-50 -o jflex-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-500 -o jflex-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-1000 -o jflex-1000_graph

java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-10 -o rpki-commons-10_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-50 -o rpki-commons-50_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-500 -o rpki-commons-500_graph
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-1000 -o rpki-commons-1000_graph
