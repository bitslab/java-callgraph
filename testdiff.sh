cd output
for i in `ls *-reachability.png`;
do
    echo Processing "$i"...
    diffimg ../artifacts/expected/"$i" "$i"
    echo Done
done