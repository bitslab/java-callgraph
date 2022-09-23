for i in `ls output/*-reachability.png`;
do
    echo Processing "$i"...
    image=${i:7}
    python3 -m diffimg artifacts/expected/"$1"/"$image" "$i"
    echo Done
done
echo Completed diffimg testing