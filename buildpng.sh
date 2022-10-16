cd output
for i in `ls *-reachability.dot`;
do
    echo Processing "$i"...
    output=${i%.dot}
    dot -Tpng -o "$output".png "$i"
    echo Done
done
mv *.png "$1"
rm *.dot
echo Completed generating png files