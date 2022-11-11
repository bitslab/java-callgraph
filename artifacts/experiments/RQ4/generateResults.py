import datetime
import os
import re

import numpy as np
import pandas as pd

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["jflex", "convex", "mph-table"]
REPORT_NAME = "artifacts/output/rq4.csv"
TEX_REPORT_NAME = "artifacts/output/rq4.tex"

CALC_NAMES = ['Vanilla', 'Improved']

propertyShortNames = {
    "TestSmartByteSerializer#canRoundTripBytes": 'byte',
    "TestSmartIntegerSerializer#canRoundTripIntegers": 'int',
    "TestSmartListSerializer#canRoundTripSerializableLists": 'list',
    "TestSmartLongSerializer#canRoundTripLongs": 'long',
    "TestSmartOptionalSerializer#canRoundTripPresentOptionals": 'optionals',
    "TestSmartPairSerializer#canRoundTripPairs": 'pair',
    "TestSmartShortSerializer#canRoundTripShort": 'short',
    "TestSmartStringSerializer#canRoundTripStrings": 'string',
    "TestSmartListSerializer#canRoundTripSerializableListsWithGenerator": 'list*',
    "GenTestFormat#dataRoundTrip": 'data',
    "GenTestFormat#messageRoundTrip": 'message',
    "GenTestFormat#primitiveRoundTrip": 'primitive',
    "CharClassesQuickcheck#addSet": 'addSet',
    "CharClassesQuickcheck#addSingle": 'addSingle',
    "CharClassesQuickcheck#addSingleSingleton": 'addSingleton',
    "CharClassesQuickcheck#addString": 'addString',
    "StateSetQuickcheck#addStateDoesNotRemove": 'add',
    "StateSetQuickcheck#containsElements": 'contains',
    "StateSetQuickcheck#removeAdd": 'remove'
}

row_count = 1

def obtain_stats_directories(results_directory: str) -> list[str]:
    directory_tree = [x for x in os.walk(results_directory)] # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]

def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> dict[str, str]:
    valid_directories = []
    project_string = project_name if project_name != "convex" else project_name + "-core"  # edge case
    if "mph-table-fixed" in stats_directories[0]:  # edge case
        project_string = "mph-table-fixed"
    time_stamps = [datetime.datetime.strptime(x.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
                   for x in stats_directories]
    time_stamps.sort()
    valid_runs = time_stamps[-10:]
    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
        if val in valid_runs:
            valid_directories.append(directory)
    return valid_directories

def evaluate_directories(project_name: str, results_directory: str, directories: list[str])-> dict[str, dict]:
    final_stats = {}
    iteration = 1
    for directory in directories:
        directory_path = results_directory + directory + "/"
        directory_tree = [x[2] for x in os.walk(directory_path)]
        valid_htmls = [x for x in directory_tree[0] if 'html' in x]
        directory_stats = retrieve_time_elapsed(directory_path=directory_path, valid_htmls=valid_htmls)
        project_iteration = project_name + " - " + str(iteration)
        final_stats[project_iteration] = directory_stats
        iteration += 1
    return final_stats

def retrieve_time_elapsed(directory_path: str, valid_htmls: list[str]) -> dict[str, str]:
    times_elapsed_dict = {}
    for html_file in valid_htmls:
        property_name = html_file.replace(".html", "")
        property_short_name = propertyShortNames[property_name]
        file_path = directory_path + html_file
        with open(file_path) as f:
            contents = f.read()
            time_elapsed_regrex = re.search('Total Time Elapsed: (.+?) seconds', contents)
            if time_elapsed_regrex:
                time_elapsed = time_elapsed_regrex.group(1)
                times_elapsed_dict[property_short_name] = round(float(time_elapsed), 2)
    return times_elapsed_dict

def generate_report_stats(stat_values: dict[str, dict]) -> dict[str, str]:
    first_iteration = stat_values[next(iter(stat_values))]
    # stage a dictionary to contain an array of times for ea property
    property_dict = {}
    for key in first_iteration:
        property_dict[key] = []

    # populate the dictionary with our results
    for key, val in stat_values.items():
        for prop, time in val.items():
            property_array = property_dict.get(prop)
            property_array.append(time)

    # generate mean, standard deviation and populate our final object
    property_stats_dict = {}
    for key, val in property_dict.items():
        np_array = np.array(val)
        mean = round(np_array.mean(), 2)
        standard_dev = round(np_array.std(), 2)
        property_stats_dict[key] = str(mean) + " \u00B1 " + str(standard_dev)
    return property_stats_dict


def generate_project_report(project_name: str, final_stats: dict[str, str], final_fixed_stats: dict[str, str]) -> dict[str, dict]:
    final_report_dict = {project_name: final_stats, project_name + "-fixed": final_fixed_stats}
    return final_report_dict


def generate_project_df(final_stats: dict[str, str], final_fixed_stats: dict[str, str]) -> pd.DataFrame():
    vanilla_df = pd.DataFrame()
    vanilla_df['Property'] = [key for key in final_stats.keys()]
    vanilla_df['Vanilla'] = [val for val in final_stats.values()]

    improved_df = pd.DataFrame()
    improved_df['Property'] = [key for key in final_fixed_stats.keys()]
    improved_df['Improved'] = [val for val in final_fixed_stats.values()]

    merged_df = pd.merge(vanilla_df, improved_df, how='outer', on='Property')
    merged_df['N'] = pd.RangeIndex(start=row_count, stop=len(merged_df.index) + row_count)

    final_df = merged_df[['N', 'Property', 'Vanilla', 'Improved']]
    return final_df


def main():
    final_dataset = {}
    for project_name in PROJECTS:
        fixed_project_name = project_name + "-fixed"
        results_directory = BASE_RESULT_DIR + project_name + "/"
        fixed_results_directory = BASE_RESULT_DIR + fixed_project_name + "/"
        # vanilla
        stats_directories = obtain_stats_directories(results_directory=results_directory)
        evaluated_runs = filter_for_recent_results(project_name=project_name, stats_directories=stats_directories)
        raw_stats = evaluate_directories(project_name=project_name, results_directory=results_directory, directories=evaluated_runs)

        # fixed
        fixed_stats_directories = obtain_stats_directories(results_directory=fixed_results_directory)
        evaluated_fixed_runs = filter_for_recent_results(project_name=project_name, stats_directories=fixed_stats_directories)
        fixed_raw_stats = evaluate_directories(project_name=fixed_project_name, results_directory=fixed_results_directory, directories=evaluated_fixed_runs)

        # obtain mean/st dev
        final_stats = generate_report_stats(stat_values=raw_stats)
        final_fixed_stats = generate_report_stats(stat_values=fixed_raw_stats)
        project_df = generate_project_df(final_stats=final_stats, final_fixed_stats=final_fixed_stats)
        final_dataset[project_name] = project_df


    with open(TEX_REPORT_NAME, 'w') as tf:
        df = pd.DataFrame()
        for project in PROJECTS:
            final_dataset[project]['_style'] = ''
            proj_mean_and_std = final_dataset[project][CALC_NAMES].copy()
            vanilla_mean = pd.DataFrame(proj_mean_and_std['Vanilla'].apply(lambda v: float(v.split(" \u00B1 ")[0]) if
                                                                " \u00B1 " in str(v) else np.nan)).reset_index()
            improved_mean = pd.DataFrame(proj_mean_and_std['Improved'].apply(lambda v: float(v.split(" \u00B1 ")[0]) if
                                                                " \u00B1 " in str(v) else np.nan)).reset_index()

            proj_mean = pd.merge(vanilla_mean, improved_mean, how='outer', on='index')[CALC_NAMES].mean()
            proj_mean['_style'] = 'BOLD'
            proj_mean['N'] = ''
            proj_mean['Property'] = 'Average'
            final_dataset[project].loc['mean'] = proj_mean

            header = dict(zip(['N', 'Property', 'Vanilla', 'Improved'], ['', '', '', '']))
            df = pd.concat([
                df,
                pd.DataFrame(header | {'_style': 'HEADER', 'Property': project}, index=[0]),
                final_dataset[project]
            ], ignore_index=True)
            # break
        bold_rows = df[ df['_style'] == 'BOLD' ].index
        header_rows = df[ df['_style'] == 'HEADER' ].index

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
                outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{' + s[1].strip()[7:].strip() + '}' + " \\\\\n" + '\\hline' + "\n"
            else:
                outTable += line

        tf.write(outTable)



if __name__ == "__main__":
    main()
