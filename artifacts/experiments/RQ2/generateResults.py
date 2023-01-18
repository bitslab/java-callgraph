import datetime
import os
import re

import numpy as np
import pandas as pd

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["mph-table"]
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
    directory_tree = [x for x in os.walk(results_directory)] # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]

def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> dict[str, str]:
    valid_directories = []
    project_string = project_name if project_name != "convex" else project_name + "-core"  # edge case
    time_stamps = [datetime.datetime.strptime(x.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
                   for x in stats_directories]
    time_stamps.sort()
    valid_runs = time_stamps[-10:]
    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
        if val in valid_runs:
            valid_directories.append(directory)
    return valid_directories

def main():
    return 0

if __name__ == "__main__":
    main()
