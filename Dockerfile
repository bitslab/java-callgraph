FROM maven:3.8.6-eclipse-temurin-11-alpine

# if work dir changed, the RUN sed line needs changes also
WORKDIR /root

# add needing packages
RUN apk add --no-cache git sed patch graphviz

RUN mkdir -p -m 700 artifact/
RUN mkdir -p -m 700 git/

# copy java-callgraph and repos
COPY git/java-callgraph artifact/
COPY git/mph-table/ git/mph-table/

# custom prompt
RUN echo PS1=\"[\\u@artifact-java-callgraph \\W]\\$ \" > .bashrc

# alter artifact files to point git locally instead
RUN sed -i 's/https:\/\/github.com\/indeedeng\/mph-table.git/\/root\/git\/mph-table/g' "artifact/artifacts/configs/mph-table/mph-table.yaml"


ENTRYPOINT ["/bin/bash"]
