cd output
for i in `ls *-reachability.dot`;
do
    echo Processing "$i"...
    output=${i%.dot}
    dot -Tpng -o "$output".png "$i"
    echo Done
done
echo Completed generating png files