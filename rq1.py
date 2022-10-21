import pandas as pd
from numpy import genfromtxt

#convert Y/N to True/False
data = pd.read_csv('artifacts/experiment/rq1_mph-table.csv', sep=',', header=0)
data['inJaCoCo'] = data['inJaCoCo'] == "Y"
data['inPrunedGraph'] = data['inPrunedGraph'] == "Y"

data['FP'] = (data['inJaCoCo'] | ~data['inPrunedGraph'])
data['FP'].apply(lambda v: 1 if v else 0)

data['FN'] = (~data['inJaCoCo'] & data['inPrunedGraph'])
data['FN'].apply(lambda v: 1 if v else 0)

df2 = data[['entryPoint','FP','FN']].groupby('entryPoint').sum()
print(df2)

