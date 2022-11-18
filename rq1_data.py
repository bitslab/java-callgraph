import pandas as pd
from common import shortNames

FIELD_N = 'N'
FIELD_PROPERTY = 'Property'
FIELD_JACOCO = '\\jacoco'
FIELD_SYSNAME = '\\sysname'
FIELD_REACHABLE = 'Reachable'
FIELD_IMPOSSIBLE = 'Impossible'
FIELD_MISSED = 'Missed'

PROP_NAMES = [FIELD_N, FIELD_PROPERTY]
CALC_NAMES = [FIELD_JACOCO, FIELD_IMPOSSIBLE, FIELD_MISSED, FIELD_SYSNAME]
TABLE_HEADER = PROP_NAMES + CALC_NAMES

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
    data[FIELD_PROPERTY] = data['entryPoint'].apply(lambda v: shortNames[v])

    df = data[[FIELD_PROPERTY, 'FP', 'FN', 'TP']].groupby(by=FIELD_PROPERTY).sum().round(2)
    df[FIELD_JACOCO] = df['FP'] + df['TP']
    df[FIELD_REACHABLE] = df['TP']
    df[FIELD_IMPOSSIBLE] = df['FP']
    df[FIELD_MISSED] = df['FN']
    df[FIELD_SYSNAME] = df['FN'] + df['TP']
    df[FIELD_N] = pd.RangeIndex(start=rowCount, stop=len(df.index) + rowCount)
    df.reset_index(inplace=True)
    dfSubset = df[TABLE_HEADER]

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
        projMean[FIELD_N] = ''
        projMean[FIELD_PROPERTY] = 'Average'
        dataSetSum[projName].loc['mean'] = projMean

        header = dict(zip(TABLE_HEADER, map(lambda v: '', TABLE_HEADER)))

        newDF = pd.concat([
            newDF,
            pd.DataFrame(header | {'_style': 'HEADER', FIELD_PROPERTY: projName}, index=[0]), # project header
            dataSetSum[projName] # project data / avg
        ], ignore_index=True)

    bold_rows = newDF[ newDF['_style'] == 'BOLD' ].index
    header_rows = newDF[ newDF['_style'] == 'HEADER' ].index
    data_rows = newDF[ newDF['_style'] != 'HEADER' ].index

    latexTable = newDF \
        .drop(columns=['_style']) \
        .style \
        .hide(axis=0) \
        .format({
                FIELD_JACOCO: "{:.0f}",
                FIELD_IMPOSSIBLE: "-{:.0f}",
                FIELD_MISSED: "+{:.0f}",
                FIELD_SYSNAME: "{:.0f}"
            }, subset=pd.IndexSlice[data_rows, :]) \
        .set_properties(subset=pd.IndexSlice[header_rows, :], **{'HEADER': ''}) \
        .set_properties(subset=pd.IndexSlice[bold_rows, :], **{'textbf': '--rwrap'}) \
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