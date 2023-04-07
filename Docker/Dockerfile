FROM maven:3.8.6-eclipse-temurin-11-alpine

# if work dir changed, the RUN sed line needs changes also
WORKDIR /root

# add needing packages
RUN apk add --no-cache git sed patch graphviz python3 py3-pip
RUN pip3 install setuptools
RUN pip3 install numpy
RUN pip3 install pandas

RUN mkdir -p -m 700 artifact/
RUN mkdir -p -m 700 git/

# copy java-callgraph and repos
#COPY java-callgraph artifact/
RUN git clone https://github.com/bitslab/java-callgraph.git git/java-callgraph
RUN cd git/java-callgraph && mvn package
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-fixed
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-50
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-500
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-1000
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c mph-table-naive
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-10
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-50
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-500
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-1000
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c convex-fixed
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-10
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-50
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-500
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-1000
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c jflex-fixed
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-10
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-50
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-500
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-1000
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar git -c rpki-commons-fixed
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table -o mph-table_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-fixed -o mph-table-fixed_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-10 -o mph-table-10_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-50 -o mph-table-50_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-500 -o mph-table-500_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-1000 -o mph-table-1000_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c mph-table-naive -o mph-table-naive_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex -o convex_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-fixed -o convex-fixed_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-10 -o convex-10_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-50 -o convex-50_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-500 -o convex-500_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c convex-1000 -o convex-1000_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex -o jflex_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-fixed -o jflex-fixed_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-10 -o jflex-10_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-50 -o jflex-50_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-500 -o jflex-500_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c jflex-1000 -o jflex-1000_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons -o rpki-commons_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-10 -o rpki-commons-10_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-50 -o rpki-commons-50_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-500 -o rpki-commons-500_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-1000 -o rpki-commons-1000_graph
RUN cd git/java-callgraph && java -jar target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar build -c rpki-commons-fixed -o rpki-commons-fixed_graph


# custom prompt
RUN echo PS1=\"[\\u@artifact-java-callgraph \\W]\\$ \" > .bashrc

# alter artifact files to point git locally instead
#RUN sed -i 's/https:\/\/github.com\/indeedeng\/mph-table.git/\/root\/git\/mph-table/g' "artifact/artifacts/configs/mph-table/mph-table.yaml"


ENTRYPOINT ["/bin/bash"]