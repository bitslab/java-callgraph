import datetime
import os
import pandas as pd
import numpy as np
import re

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["convex", "jflex", "mph-table", "rpki-commons"]
REPORT_NAME = "artifacts/output/rq2.csv"
TEX_REPORT_NAME = "artifacts/output/rq2.tex"
ITERATIONS = [10, 50, 500, 1000]
RAW_NAMES = ["Property", "10", "50", "100", "500", "1000"]
row_count = 1

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


def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> list[str]:
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
    valid_runs = time_stamps[-10:]
    valid_directories = []
    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_string, "").replace("_", ":").replace("T", " "),
                                         "%Y-%m-%d %H:%M:%S.%f")
        if val in valid_runs:
            valid_directories.append(directory)
    return valid_directories

def calculate_coverage(file: str) -> int:
    coverage: int = 0
    with open(file) as f:
        lines = [line.rstrip() for line in f]
        # nodes_covered = int(lines[1].replace("nodesCovered,", ""))
        # node_count = int(lines[2].replace("nodeCount,", ""))
        lines_covered = int(lines[3].replace("linesCovered,", ""))
        # lines_missed = int(lines[4].replace("linesMissed,", ""))

        coverage = lines_covered
        # coverage["LC"] = lines_covered / (lines_covered + lines_missed) * 100
    return coverage


def obtain_time_elapsed(time_file: str) -> float:
    with open(time_file) as f:
        contents = f.read()
        time_elapsed_regrex = re.search('Total Time Elapsed: (.+?) seconds', contents)
        if time_elapsed_regrex:
            time_elapsed = time_elapsed_regrex.group(1)
            return round(float(time_elapsed), 2)
    return -1.00


def obtain_iteration_stats(iteration_directories: list[str]) -> dict[str, tuple]:
    stats: dict[str, list] = {}
    times: dict[str, list] = {}
    for iteration_directory in iteration_directories:
        files = [x for x in os.walk(
            iteration_directory)][0][2]
        stats_files = list(filter(lambda stat_file: "reachability-coverage.csv" in stat_file, files))
        time_files = [f.replace("-reachability-coverage.csv", ".html") for f in stats_files]
        for file, time_file in zip(stats_files, time_files):
            file_location = iteration_directory + "/" + file
            time_file_location = iteration_directory + "/" + time_file
            prop = file.replace("-reachability-coverage.csv", "")
            if prop not in stats:
                stats[prop] = []
                times[prop] = []
            stats[prop].append(calculate_coverage(file=file_location))
            times[prop].append(obtain_time_elapsed(time_file=time_file_location))
    ret = {}
    for key, val in stats.items():
        np_array_stats = np.array(val)
        mean_stats = '{:.2f}'.format(round(np_array_stats.mean(), 2))
        standard_dev_stats = '{:.2f}'.format(round(np_array_stats.std(), 2))
        time_val = times[key]
        np_array_times = np.array(time_val)
        mean_times = '{:.2f}'.format(round(np_array_times.mean(), 2))
        standard_dev_times = '{:.2f}'.format(round(np_array_times.std(), 2))

        stats_str = str(mean_stats) + " \u00B1 " + str(standard_dev_stats)
        times_str = str(mean_times) + " \u00B1 " + str(standard_dev_times)
        ret[prop] = (stats_str, times_str)
    return ret


def generate_project_df(project_ds: dict[int, dict], row_count: int) -> pd.DataFrame():
    property_dict = {}
    valid_keys = project_ds[10].keys()  # grab first dict keys for property names
    for key in valid_keys:
        property_dict[key] = []
        for trial in project_ds.keys():
            if key not in project_ds[trial]:
                property_dict[key].append((np.nan, np.nan))
            else:
                property_dict[key].append(project_ds[trial][key])

    project_df = pd.DataFrame()
    for prop, val in property_dict.items():
        print(val)
        mc_dict = {"N": row_count, "Property": propertyShortNames[prop], 10: val[1][0],
                   50: val[2][0], 100: val[0][0], 500: val[3][0], 1000: val[4][0]}
        row_count += 1
        tt_dict = {"N": row_count, "Property": "time(s)", 10: val[1][1],
                   50: val[2][1], 100: val[0][1], 500: val[3][1], 1000: val[4][1]}
        method_coverage_df = pd.DataFrame(mc_dict, index=[i for i in range(1)])
        time_taken_df = pd.DataFrame(tt_dict, index=[i for i in range(1)])

        project_df = pd.concat([project_df, method_coverage_df, time_taken_df])
    return project_df


def main():
    final_dataset = {}
    for project in PROJECTS:
        project_dataset = {}
        stats_directory_base = BASE_RESULT_DIR + project + "/"
        project_base_iteration_stats = obtain_stats_directories(results_directory=stats_directory_base)
        filtered_results_base = filter_for_recent_results(project_name=project,
                                                          stats_directories=project_base_iteration_stats)
        iteration_directories_base = [stats_directory_base + result for result in filtered_results_base]
        iteration_stats = obtain_iteration_stats(iteration_directories=iteration_directories_base)
        project_dataset[100] = iteration_stats
        for iteration in ITERATIONS:
            project_name = project + "-" + str(iteration)
            stats_directory = BASE_RESULT_DIR + project_name + "/"
            project_iteration_stats = obtain_stats_directories(results_directory=stats_directory)
            filtered_results = filter_for_recent_results(project_name=project_name,
                                                         stats_directories=project_iteration_stats)
            iteration_directories = [stats_directory + result for result in filtered_results]
            iteration_stats = obtain_iteration_stats(iteration_directories=iteration_directories)
            project_dataset[iteration] = iteration_stats
        final_dataset[project] = generate_project_df(project_ds=project_dataset, row_count=row_count)
    print(final_dataset)

    with open(TEX_REPORT_NAME, 'w') as tf:
        df = pd.DataFrame()
        for project in PROJECTS:
            final_dataset[project]['_style'] = ''
            header = dict(zip(['N', 'Property', 10, 50, 100, 500, 1000], ['', '', '', '', '', '', '']))
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
                outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{\\' + s[1].strip()[
                                                                                 7:].strip().replace("-",
                                                                                                     "") + '}' + " \\\\\n" + '\\hline' + "\n"
            else:
                outTable += line

        tf.write(outTable)


if __name__ == "__main__":
    main()
