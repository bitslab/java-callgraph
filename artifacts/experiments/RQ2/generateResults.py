import datetime
import os
import pandas as pd
import numpy as np
import re

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["convex", "jflex", "rpki-commons"]
REPORT_NAME = "artifacts/output/rq2.csv"
TEX_REPORT_NAME = "artifacts/output/rq2.tex"

ITERATIONS = [10, 50, 500, 1000]
RAW_NAMES = ["Property", "10", "50", "500", "1000"]

propertyShortNames = {
    "TestSmartListSerializer#canRoundTripSerializableLists": 'list',
    "GenTestFormat#dataRoundTrip": 'data',
    "GenTestFormat#messageRoundTrip": 'message',
    "GenTestFormat#primitiveRoundTrip": 'primitive',
    "CharClassesQuickcheck#addSet": 'addSet',
    "CharClassesQuickcheck#addSingle": 'addSingle',
    "CharClassesQuickcheck#addSingleSingleton": 'addSingleton',
    "CharClassesQuickcheck#addString": 'addString',
    "StateSetQuickcheck#addStateDoesNotRemove": 'add',
    "StateSetQuickcheck#containsElements": 'contains',
    "StateSetQuickcheck#removeAdd": 'remove',
    "X509ResourceCertificateParentChildValidatorTest#validParentChildSubResources": 'resources'
}


def obtain_stats_directories(results_directory: str) -> list[str]:
    directory_tree = [x for x in os.walk(
        results_directory)]  # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]


def filter_for_recent_result(project_name: str, stats_directories: list[str]) -> str:
    if "convex" in project_name:
        project_string = project_name.split("-")[0] + "-core"
    elif "jflex" in project_name:
        project_string = "jflex"
    else:
        project_string = project_name

    time_stamps = [datetime.datetime.strptime(x.replace(project_string, "").replace("_", ":").replace("T", " "),
                                              "%Y-%m-%d %H:%M:%S.%f")
                   for x in stats_directories]
    time_stamps.sort()
    valid_runs = time_stamps[-1:]

    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_string, "").replace("_", ":").replace("T", " "),
                                         "%Y-%m-%d %H:%M:%S.%f")
        if val in valid_runs:
            return directory


def calculate_coverage(file: str) -> float:
    coverage: float = 0.00
    with open(file) as f:
        lines = [line.rstrip() for line in f]
        nodes_covered = int(lines[1].replace("nodesCovered,", ""))
        node_count = int(lines[2].replace("nodeCount,", ""))
        # lines_covered = int(lines[3].replace("linesCovered,", ""))
        # lines_missed = int(lines[4].replace("linesMissed,", ""))

        coverage = nodes_covered / node_count * 100
        # coverage["LC"] = lines_covered / (lines_covered + lines_missed) * 100

    return round(coverage, 2)


def obtain_time_elapsed(time_file: str) -> float:
    with open(time_file) as f:
        contents = f.read()
        time_elapsed_regrex = re.search('Total Time Elapsed: (.+?) seconds', contents)
        if time_elapsed_regrex:
            time_elapsed = time_elapsed_regrex.group(1)
            return round(float(time_elapsed), 2)
    return -1.00


def obtain_iteration_stats(iteration_directory: str) -> dict[str, tuple]:
    files = [x for x in os.walk(
        iteration_directory)][0][2]
    stats_files = list(filter(lambda stat_file: "reachability-coverage.csv" in stat_file, files))
    time_files = [f.replace("-reachability-coverage.csv", ".html") for f in stats_files]
    ret = {}
    for file, time_file in zip(stats_files, time_files):
        file_location = iteration_directory + "/" + file
        time_file_location = iteration_directory + "/" + time_file
        prop = file.replace("-reachability-coverage.csv", "")
        ret[propertyShortNames[prop]] = (
        calculate_coverage(file=file_location), obtain_time_elapsed(time_file=time_file_location))
    return ret


def generate_project_df(project_ds: dict[int, dict]) -> pd.DataFrame():
    project_df = pd.DataFrame()
    valid_keys = project_ds[10].keys()  # grab first dict keys for property names
    project_df['Property'] = [key for key in valid_keys]
    for key in project_ds.keys():
        iteration_property_dict = project_ds[key]
        for vk in valid_keys:
            if vk not in iteration_property_dict:
                iteration_property_dict[vk] = (np.nan, np.nan)
        project_df[key] = [val for val in project_ds[key].values()]
        # coverage = []
        # times = []
        # for k, v in iteration_property_dict.items():
        #     coverage.append(v[0])
        #     times.append(v[1])
        #
        # project_df[key] = [c for c in coverage]
        # project_df[key].loc['Time (sec)'] = [t for t in times]
    return project_df


def main():
    final_dataset = {}
    row_count = 1
    for project in PROJECTS:
        project_dataset = {}
        for iteration in ITERATIONS:
            project_name = project + "-" + str(iteration)
            stats_directory = BASE_RESULT_DIR + project_name + "/"
            project_iteration_stats = obtain_stats_directories(results_directory=stats_directory)
            iteration_directory = stats_directory + filter_for_recent_result(project_name=project_name,
                                                                             stats_directories=project_iteration_stats)
            iteration_stats = obtain_iteration_stats(iteration_directory=iteration_directory)
            project_dataset[iteration] = iteration_stats
        final_dataset[project] = generate_project_df(project_ds=project_dataset)
    print(final_dataset)

    with open(TEX_REPORT_NAME, 'w') as tf:
        df = pd.DataFrame()
        for project in PROJECTS:
            final_dataset[project]['_style'] = ''
            header = dict(zip(['N', 'Property', '10', '50', '100', '500', '1000'], ['', '', '', '', '', '', '']))
            final_dataset[project]['N'] = pd.RangeIndex(start=row_count,
                                                        stop=len(final_dataset[project].index) + row_count)
            df = pd.concat([
                df,
                pd.DataFrame(header | {'_style': 'HEADER', 'Property': project}, index=[0]),
                final_dataset[project]
            ], ignore_index=True)

        bold_rows = df[df['_style'] == 'BOLD'].index
        header_rows = df[df['_style'] == 'HEADER'].index
        latexTable = df \
            .drop(columns=['_style']) \
            .style \
            .hide(axis=0) \
            .format(precision=2) \
            .set_properties(subset=pd.IndexSlice[header_rows, :], **{'HEADER': ''}) \
            .set_properties(subset=pd.IndexSlice[bold_rows, :], **{'textbf': '--rwrap'}) \
            .to_latex(hrules=False)

        outTable = ''

        # transform to sub headers
        for line in latexTable.splitlines(keepends=True):
            s = line.split('&')
            c = str(len(s))

            possibleCommand = s[0].strip()

            if possibleCommand == '\HEADER':
                outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{' + s[1].strip()[
                                                                               7:].strip() + '}' + " \\\\\n" + '\\hline' + "\n"
            else:
                outTable += line

        tf.write(outTable)


if __name__ == "__main__":
    main()
