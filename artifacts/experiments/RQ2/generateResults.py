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

    return coverage


def obtain_iteration_stats(iteration_directory: str) -> dict[str, float]:
    files = [x for x in os.walk(
        iteration_directory)][0][2]
    stats_files = list(filter(lambda stat_file: "reachability-coverage.csv" in stat_file, files))
    ret = {}
    for file in stats_files:
        file_location = iteration_directory + "/" + file
        prop = file.replace("-reachability-coverage.csv", "")
        ret[prop] = calculate_coverage(file=file_location)
    return ret


def generate_project_df(project_ds: dict[int, dict]) -> pd.DataFrame():
    print(project_ds)
    project_df = pd.DataFrame()
    property_dict = project_ds[10]  # grab first dict for property names
    project_df['Property'] = [key for key in property_dict.keys()]
    for key in project_ds.keys():
        project_df[key] = [val if val else np.nan for val in project_ds[key].values()]
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


if __name__ == "__main__":
    main()
