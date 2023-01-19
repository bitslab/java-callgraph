import datetime
import os
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
        project_string = project_name + "-core"
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


def main():
    final_dataset = {}
    row_count = 1
    for project in PROJECTS:
        project_dataset = {}
        for iteration in ITERATIONS:
            project_name = project + "-" + str(iteration)
            stats_directory = BASE_RESULT_DIR + project_name + "/"
            project_iteration_stats = obtain_stats_directories(results_directory=stats_directory)
            iteration_directory = stats_directory + filter_for_recent_result(project_name=project_name, stats_directories=project_iteration_stats)


if __name__ == "__main__":
    main()
