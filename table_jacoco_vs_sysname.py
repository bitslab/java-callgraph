import pandas as pd
from common import shortNames

FIELD_PROPERTY = 'Property'
FIELD_JACOCO = '\\jacoco'
FIELD_SYSNAME = '\\sysname'
FIELD_REACHABLE = 'Reachable'
FIELD_IMPOSSIBLE = 'Impossible'
FIELD_MISSED = 'Missed'
FIELD_FIRST = 'First'
FIELD_SECOND = 'Second'
FIELD_THIRD = 'Third'

PROP_NAMES = [FIELD_PROPERTY]
CALC_NAMES = [FIELD_JACOCO, FIELD_IMPOSSIBLE, FIELD_MISSED, FIELD_SYSNAME, FIELD_FIRST, FIELD_SECOND, FIELD_THIRD]
TABLE_HEADER = PROP_NAMES + CALC_NAMES

projects = [
  ('convex', 'artifacts/experiment/rq1_convex.csv', 'artifacts/experiment/rq1_paths_convex.csv', 'artifacts/experiment/table_jacoco_vs_sysname_convex.tex'),
  ('jflex', 'artifacts/experiment/rq1_jflex.csv', 'artifacts/experiment/rq1_paths_jflex.csv', 'artifacts/experiment/table_jacoco_vs_sysname_jflex.tex'),
  ('mphtable', 'artifacts/experiment/rq1_mph-table.csv', 'artifacts/experiment/rq1_paths_mph-table.csv', 'artifacts/experiment/table_jacoco_vs_sysname_mph-table.tex'),
  ('rpkicommons', 'artifacts/experiment/rq1_rpki-commons.csv', 'artifacts/experiment/rq1_paths_rpki-commons.csv', 'artifacts/experiment/table_jacoco_vs_sysname_rpki-commons.tex'),
]

byProjNameFile = 'artifacts/experiment/table_jacoco_vs_sysname_projects.tex'

byAllEntrypointNameFile = 'artifacts/experiment/table_jacoco_vs_sysname_all_entrypoints.tex'

dataSet = pd.DataFrame()
dataSetSum = {}
rowCount = 1

for project in projects:
    projName = project[0]
    csvFile = project[1]
    csvPaths = project[2]
    texFile = project[3]

    dataPaths = pd.read_csv(csvPaths, sep=',', header=0)
    dataPaths['Project'] = projName

    data = pd.read_csv(csvFile, sep=',', header=0)
    data['Project'] = projName
    data['reachableJaCoCo'] = data['inJaCoCo'] == "Y"  # convert Y/N to True/False
    data['reachableProperty'] = data['inPrunedGraph'] == "Y"  # convert Y/N to True/False
    data['linesTotal'] = pd.to_numeric(data['linesCovered'], errors='coerce').fillna(0) \
                         + pd.to_numeric(data['linesMissed'], errors='coerce').fillna(0)

    # false-positives: tool identifies code as reachable,
    #   but cannot be reached by a property test
    data['FP'] = (data['reachableJaCoCo'] & ~data['reachableProperty'])

    # false-negatives: code that is reachable from the property
    #   test but the tool does not identify it as such
    data['FN'] = (~data['reachableJaCoCo'] & data['reachableProperty'])

    # JaCoCo and our tool agree that is reachability
    data['TP'] = (data['reachableJaCoCo'] & data['reachableProperty'])

    # JaCoCo and our tool agree that is NOT reachable
    data['TN'] = (~data['reachableJaCoCo'] & ~data['reachableProperty'])

    # Add values to the correct columns for each row
    data[FIELD_JACOCO] = data['linesTotal']
    data.loc[(~(data['FP'] | data['TP'])), FIELD_JACOCO] = 0

    data[FIELD_IMPOSSIBLE] = data['linesTotal']
    data.loc[(~(data['FP'])), FIELD_IMPOSSIBLE] = 0

    data[FIELD_REACHABLE] = data['linesTotal']
    data.loc[(~(data['TP'])), FIELD_REACHABLE] = 0

    data[FIELD_MISSED] = data['linesTotal']
    data.loc[(~(data['FN'])), FIELD_MISSED] = 0

    data[FIELD_SYSNAME] = data['linesTotal']
    data.loc[(~(data['FN'] | data['TP'])), FIELD_SYSNAME] = 0

    # add Name as a friendly name for each entrypoint
    data[FIELD_PROPERTY] = data['entryPoint'].apply(lambda v: shortNames[v])
    dataPaths[FIELD_PROPERTY] = dataPaths['entryPoint'].apply(lambda v: shortNames[v])

    dfGrouped = data[[FIELD_PROPERTY, FIELD_JACOCO, FIELD_IMPOSSIBLE, FIELD_REACHABLE, FIELD_MISSED, FIELD_SYSNAME]].groupby(by=FIELD_PROPERTY).sum().round(2)
    df = dfGrouped.merge(dataPaths[[FIELD_PROPERTY, 'First', 'Second', 'Third']], on=FIELD_PROPERTY, how='left')

    df.reset_index(inplace=True)
    dfSubset = df[TABLE_HEADER]

    rowCount = len(df.index) + rowCount
    dataSetSum[projName] = dfSubset.copy()

    with open(texFile, 'w') as tf:
        tf.write(dfSubset.style.hide(axis="index").to_latex())

    dataSet = pd.concat([dataSet, data.copy()])

# output sum group by projName
with open(byProjNameFile, 'w') as tf:
    fpfnSum = dataSet[['Project', FIELD_JACOCO, FIELD_IMPOSSIBLE, FIELD_REACHABLE, FIELD_MISSED, FIELD_SYSNAME]] \
        .sort_values(by='Project') \
        .groupby(by='Project') \
        .sum()

    fpfnSum['Total'] = dataSet[['Project']].groupby(by='Project').size()
    tf.write(fpfnSum.reset_index().style.hide(axis="index").to_latex())

# output all projects with project headings
with open(byAllEntrypointNameFile, 'w') as tf:
    newDF = pd.DataFrame()

    for project in projects:
        projName = project[0]
        dataSetSum[projName]['_style'] = ''

        projMean = dataSetSum[projName][CALC_NAMES].fillna(0).mean().round()
        projMean['_style'] = 'BOLD'
        projMean[FIELD_PROPERTY] = 'Average'
        dataSetSum[projName].loc['mean'] = projMean

        header = dict(zip(TABLE_HEADER, map(lambda v: '', TABLE_HEADER)))

        newDF = pd.concat([
            newDF,
            pd.DataFrame(header | {'_style': 'HEADER', FIELD_PROPERTY: projName}, index=[0]),  # project header
            dataSetSum[projName]  # project data / avg
        ], ignore_index=True)

    bold_rows = newDF[newDF['_style'] == 'BOLD'].index
    header_rows = newDF[newDF['_style'] == 'HEADER'].index
    data_rows = newDF[newDF['_style'] != 'HEADER'].index

    impossiblePercent = newDF[FIELD_IMPOSSIBLE].apply(lambda x: "0" if x == "" else x).astype('int') / newDF[
        FIELD_JACOCO].apply(lambda x: "0" if x == "" else x).astype('int')
    newDF[FIELD_IMPOSSIBLE] = list(zip(newDF[FIELD_IMPOSSIBLE], impossiblePercent * 100))

    latexTable = newDF \
        .drop(columns=['_style']) \
        .style \
        .hide(axis=0) \
        .format({
        FIELD_JACOCO: "{:.0f}",
        FIELD_IMPOSSIBLE: lambda x: "-{:.0f} ({:.0f}\%)".format(*x),
        FIELD_MISSED: "+{:.0f}",
        FIELD_SYSNAME: "{:.0f}",
        FIELD_FIRST: "{:.0f}",
        FIELD_SECOND: "{:.0f}",
        FIELD_THIRD: "{:.0f}"
    }, subset=pd.IndexSlice[data_rows, :], na_rep="-") \
        .set_properties(subset=pd.IndexSlice[header_rows, :], **{'HEADER': ''}) \
        .set_properties(subset=pd.IndexSlice[bold_rows, :], **{'textbf': '--rwrap'}) \
        .to_latex(hrules=False, column_format="llrrrrrrr")

    outTable = ''

    # transform to sub headers
    for line in latexTable.splitlines(keepends=True):
        s = line.split('&')
        c = str(len(s))

        possibleCommand = s[0].strip()

        if possibleCommand == '\HEADER':
            outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{\\' + s[1].strip()[
                                                                             7:].strip() + '}' + " \\\\\n" + '\\hline' + "\n"
        else:
            outTable += line

    tf.write(outTable)
