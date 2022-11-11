import pandas as pd
from common import shortNames

CALC_NAMES = ['FP', 'FN', 'TP']

projects = [
    ('convex', 'artifacts/experiment/rq1_convex.csv', 'artifacts/experiment/rq1_table_convex.tex'),
    ('jflex', 'artifacts/experiment/rq1_jflex.csv', 'artifacts/experiment/rq1_table_jflex.tex'),
    ('mphtable', 'artifacts/experiment/rq1_mph-table.csv', 'artifacts/experiment/rq1_table_mph-table.tex'),
    ('rpkicommons', 'artifacts/experiment/rq1_rpki-commons.csv', 'artifacts/experiment/rq1_table_rpki-commons.tex'),
]

byProjNameFile = 'artifacts/experiment/rq1_table_projects.tex'

byAllEntrypointNameFile = 'artifacts/experiment/rq1_table_all_entrypoints.tex'

dataSet = pd.DataFrame()
dataSetSum = {}
rowCount = 1

for project in projects:
    projName = project[0]
    csvFile = project[1]
    texFile = project[2]

    data = pd.read_csv(csvFile, sep=',', header=0)
    data['Project'] = projName
    data['inJaCoCo'] = data['inJaCoCo'] == "Y"  #convert Y/N to True/False
    data['inPrunedGraph'] = data['inPrunedGraph'] == "Y"  #convert Y/N to True/False

    data['reachableJaCoCo'] = data['inJaCoCo']
    data['reachableProperty'] = data['inPrunedGraph']


    # false-positives: tool identifies code as reachable,
    #   but cannot be reached by a property test
    data['FP'] = (data['reachableJaCoCo'] & ~data['reachableProperty'])
    data['FP'] = data['FP'].apply(lambda v: 1 if v else 0)

    # false-negatives: code that is reachable from the property
    #   test but the tool does not identify it as such
    data['FN'] = (~data['reachableJaCoCo'] & data['reachableProperty'])
    data['FN'] = data['FN'].apply(lambda v: 1 if v else 0)

    # JaCoCo and our tool agree that is reachability
    data['TP'] = (data['reachableJaCoCo'] & data['reachableProperty'])
    data['TP'] = data['TP'].apply(lambda v: 1 if v else 0)

    # JaCoCo and our tool agree that is NOT reachable
    data['TN'] = (~data['reachableJaCoCo'] & ~data['reachableProperty'])
    data['TN'] = data['TN'].apply(lambda v: 1 if v else 0)

    # add Name as a friendly name for each entrypoint
    data['Property'] = data['entryPoint'].apply(lambda v: shortNames[v])

    df = data[['Property', 'FP', 'FN', 'TP']].groupby(by='Property').sum().round(2)
    df['+Ratio'] = df['FP'] / df['TP']
    df['N'] = pd.RangeIndex(start=rowCount, stop=len(df.index) + rowCount)
    df.reset_index(inplace=True)
    dfSubset = df[['N', 'Property', 'FP', 'FN', 'TP', '+Ratio']]

    rowCount = len(df.index) + rowCount
    dataSetSum[projName] = dfSubset.copy()

    with open(texFile, 'w') as tf:
        tf.write(dfSubset.style.hide(axis="index").to_latex())

    dataSet = pd.concat([dataSet, data.copy()])


# output sum group by projName
with open(byProjNameFile, 'w') as tf:
    fpfnSum = dataSet[['Project', 'FP', 'FN', 'TP']]\
        .sort_values(by='Project')\
        .groupby(by='Project')\
        .sum()

    fpfnSum['Total'] = dataSet[['Project']].groupby(by='Project').size()
    tf.write(fpfnSum.reset_index().style.hide(axis="index").to_latex())


# output all projects with project headings
with open(byAllEntrypointNameFile, 'w') as tf:
    newDF = pd.DataFrame()

    for project in projects:
        projName = project[0]
        dataSetSum[projName]['_style'] = ''

        projMean = dataSetSum[projName][CALC_NAMES].mean().round()
        projMean['_style'] = 'BOLD'
        projMean['N'] = ''
        projMean['Property'] = 'Average'
        projMean['+Ratio'] = projMean['FP'] / projMean['TP']
        dataSetSum[projName].loc['mean'] = projMean

        header = dict(zip(['N', 'Property', 'FP', 'FN', 'TP', '+Ratio'], ['', '', '', '', '', '']))

        newDF = pd.concat([
            newDF,
            pd.DataFrame(header | {'_style': 'HEADER', 'Property': projName}, index=[0]), # project header
            dataSetSum[projName] # project data / avg
        ], ignore_index=True)

    bold_rows = newDF[ newDF['_style'] == 'BOLD' ].index
    header_rows = newDF[ newDF['_style'] == 'HEADER' ].index

    latexTable = newDF \
        .drop(columns=['_style']) \
        .style \
        .hide(axis=0) \
        .format(precision=0) \
        .set_properties(subset=pd.IndexSlice[header_rows, :], **{'HEADER': ''}) \
        .set_properties(subset=pd.IndexSlice[bold_rows, :], **{'textbf': '--rwrap'}) \
        .format(subset=pd.IndexSlice['+Ratio'], precision=2) \
        .to_latex(hrules=False, column_format="llrrrr")

    outTable = ''

    # transform to sub headers
    for line in latexTable.splitlines(keepends=True):
        s = line.split('&')
        c = str(len(s))

        possibleCommand = s[0].strip()

        if possibleCommand == '\HEADER':
            outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{\\' + s[1].strip()[7:].strip() + '}' + " \\\\\n" + '\\hline' + "\n"
        else:
            outTable += line

    tf.write(outTable)