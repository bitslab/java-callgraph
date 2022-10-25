import pandas as pd

projects = [
    ('MPH Table', 'artifacts/experiment/rq1_mph-table.csv', 'artifacts/experiment/rq1_table_mph-table.tex'),
    ('Convex', 'artifacts/experiment/rq1_convex.csv', 'artifacts/experiment/rq1_table_convex.tex'),
    ('jFlex', 'artifacts/experiment/rq1_jflex.csv', 'artifacts/experiment/rq1_table_jflex.tex'),
]

byProjNameFile = 'artifacts/experiment/rq1_table_projects.tex'

dataSet = pd.DataFrame()

for project in projects:
    projName = project[0]
    csvFile = project[1]
    texFile = project[2]

    data = pd.read_csv(csvFile, sep=',', header=0)
    data['Project'] = projName
    data['inJaCoCo'] = data['inJaCoCo'] == "Y"  #convert Y/N to True/False
    data['inPrunedGraph'] = data['inPrunedGraph'] == "Y"  #convert Y/N to True/False
    data['coveredJaCoCo'] = data['linesCovered'].apply(lambda v: 0 if v == "UNK" else v).astype(int) > 0

    data['FP'] = (data['coveredJaCoCo'] & ~data['inPrunedGraph'])
    data['FP'] = data['FP'].apply(lambda v: 1 if v else 0)

    data['FN'] = (~data['coveredJaCoCo'] & data['inPrunedGraph'])
    data['FN'] = data['FN'].apply(lambda v: 1 if v else 0)

    data['Agree'] = (data['coveredJaCoCo'] & data['inPrunedGraph'])
    data['Agree'] = data['Agree'].apply(lambda v: 1 if v else 0)

    with open(texFile, 'w') as tf:
        fpfnSum = data[['entryPoint', 'FP', 'FN', 'Agree']].groupby(by='entryPoint').sum()
        tf.write(fpfnSum.reset_index().style.to_latex())

    dataSet = pd.concat([dataSet, data.copy()])


# output sum group by projName
with open(byProjNameFile, 'w') as tf:
    fpfnSum = dataSet[['Project', 'FP', 'FN', 'Agree']].groupby(by='Project').sum()
    tf.write(fpfnSum.reset_index().style.hide(axis="index").to_latex())







