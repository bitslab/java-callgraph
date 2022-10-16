for i in `ls output/"$1"/*-reachability.png`;
do
    echo Processing Difference "$i"...
    image=${i:7}
    python3 configuredDiffImg.py artifacts/expected/"$image" "$i"
    echo Done
done
echo Completed diffimg testing