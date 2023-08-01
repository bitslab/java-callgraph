import pandas as pd
from common import shortNames

# IMPROVEMENT IN COVERAGE

FIELD_PROPERTY = 'Property'
FIELD_IMPROVED_LOC_COVERAGE = 'Fixed' # 'Improved LOC Coverage'
FIELD_ORIGINAL_LOC_COVERAGE = 'Vanilla' # 'Original LOC Coverage'
FIELD_LOC_COUNT_IMPROVEMENT = 'Improved' # 'Improvement'

PROP_NAMES = [FIELD_PROPERTY]
CALC_NAMES = [FIELD_IMPROVED_LOC_COVERAGE, FIELD_ORIGINAL_LOC_COVERAGE, FIELD_LOC_COUNT_IMPROVEMENT]
TABLE_HEADER = PROP_NAMES + CALC_NAMES

projects = [
    ('convex', 'artifacts/experiment/rq1_convex.csv', 'artifacts/experiment/rq1_convex-fixed.csv'),
    ('jflex', 'artifacts/experiment/rq1_jflex.csv', 'artifacts/experiment/rq1_jflex-fixed.csv'),
    ('mphtable', 'artifacts/experiment/rq1_mph-table.csv', 'artifacts/experiment/rq1_mph-table-fixed.csv'),
    ('rpkicommons', 'artifacts/experiment/rq1_rpki-commons.csv', 'artifacts/experiment/rq1_rpki-commons-fixed.csv'),
]

allCoverageFile = 'artifacts/experiment/table_coverage.tex'

dataSet = pd.DataFrame()
dataSetSum = {}
rowCount = 1

for project in projects:
    projName = project[0]
    csvFile = project[1]
    fixedCsvFile = project[2]

    original = pd.read_csv(csvFile, sep=',', header=0)
    original = original[ original['inPrunedGraph'] == "Y" ]   # only include actual reachable methods
    original[FIELD_PROPERTY] = original['entryPoint'].apply(lambda v: shortNames[v])

    fixed = pd.read_csv(fixedCsvFile, sep=',', header=0)
    fixed = fixed[ fixed['inPrunedGraph'] == "Y" ]    # only include actual reachable methods
    fixed[FIELD_PROPERTY] = fixed['entryPoint'].apply(lambda v: shortNames[v])
    fixed.rename(columns=lambda x: x if x == FIELD_PROPERTY or x == 'method' else 'FIXED_'+x, inplace=True)

    data = pd.merge(fixed, original, on=[FIELD_PROPERTY, 'method'], how='left')
    data['entryPoint'].fillna(data['FIXED_entryPoint'], inplace=True)

    # drop rows where we don't have "FIXED"
    #data = data[ data['FIXED_linesCovered'] != "UNK" ]

    data['Project'] = projName
    data[FIELD_ORIGINAL_LOC_COVERAGE] = data['linesCovered'].apply(lambda v: 0 if v == "UNK" else v).astype(float)
    data[FIELD_IMPROVED_LOC_COVERAGE] = data['FIXED_linesCovered'].apply(lambda v: 0 if v == "UNK" else v).astype(float)
    data[FIELD_LOC_COUNT_IMPROVEMENT] = 0

    # add Name as a friendly name for each entrypoint
    data[FIELD_PROPERTY] = data['entryPoint'].apply(lambda v: shortNames[v])

    df = data[[FIELD_PROPERTY]+CALC_NAMES].groupby(by=FIELD_PROPERTY).sum().round(2)
    df.reset_index(inplace=True)
    df.reset_index(inplace=True)
    dfSubset = df[PROP_NAMES + CALC_NAMES]

    rowCount = len(df.index) + rowCount
    dataSetSum[projName] = dfSubset.copy()
    dataSetSum[projName][FIELD_LOC_COUNT_IMPROVEMENT] = dataSetSum[projName][FIELD_IMPROVED_LOC_COVERAGE] - \
                                                        dataSetSum[projName][FIELD_ORIGINAL_LOC_COVERAGE]

    # show only records that have improvement
    dataSetSum[projName] = dataSetSum[projName][ dataSetSum[projName][FIELD_LOC_COUNT_IMPROVEMENT] > 0 ]

    #dataSetSum[projName][FIELD_LOC_PERCENT_IMPROVEMENT] = \
    #    dataSetSum[projName][FIELD_IMPROVED_LOC_COVERAGE] / dataSetSum[projName][FIELD_ORIGINAL_LOC_COVERAGE]

    #dataSetSum[projName][FIELD_METHOD_PERCENT_IMPROVEMENT] = \
    #    dataSetSum[projName][FIELD_IMPROVED_METHOD_COVERAGE] / dataSetSum[projName][FIELD_ORIGINAL_METHOD_COVERAGE]

    dataSet = pd.concat([dataSet, data.copy()])

# output all projects with project headings
with open(allCoverageFile, 'w') as tf:
    newDF = pd.DataFrame()

    for project in projects:
        projName = project[0]
        dataSetSum[projName]['_style'] = ''

        projMean = dataSetSum[projName][CALC_NAMES].mean()
        projMean['_style'] = 'BOLD'
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
                FIELD_IMPROVED_LOC_COVERAGE: "{:.0f}",
                FIELD_ORIGINAL_LOC_COVERAGE: "{:.0f}",
                FIELD_LOC_COUNT_IMPROVEMENT: "+{:.0f}",
            }, subset=pd.IndexSlice[data_rows, :]) \
        .set_properties(subset=pd.IndexSlice[header_rows, :], **{'HEADER': ''}) \
        .set_properties(subset=pd.IndexSlice[bold_rows, :], **{'textbf': '--rwrap'}) \
        .to_latex(hrules=False, column_format="lrrr")

    outTable = ''

    # transform to sub headers
    for line in latexTable.splitlines(keepends=True):
        s = line.split('&')
        c = str(len(s))

        possibleCommand = s[0].strip()

        if possibleCommand.startswith('\HEADER'):
            projectName = possibleCommand[7:].strip()
            outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{\\' + projectName + '}' + " \\\\\n" + '\\hline' + "\n"
        else:
            outTable += line

    tf.write(outTable)
