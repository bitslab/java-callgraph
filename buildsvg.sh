for i in `ls *-reachability.dot`; do echo -n Processing $i...; dot -Tsvg -o ${i::-4}.svg $i; echo Done; done;
for i in `ls *-annotated.dot`; do echo -n Processing $i...; dot -Tsvg -o ${i::-4}.svg $i; echo Done; done;

