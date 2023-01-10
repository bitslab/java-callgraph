import datetime
import os
import re

import numpy as np
import pandas as pd

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["convex", "jflex", "mph-table", "rpki-commons"]
REPORT_NAME = "artifacts/output/rq4.csv"
TEX_REPORT_NAME = "artifacts/output/rq4.tex"

RAW_NAMES = ['Vanilla', 'Improved']
CALC_NAMES = ['Vanilla', 'Improved', 'Overhead']

propertyShortNames = {
    "TestSmartListSerializer#canRoundTripSerializableLists": 'list',
    "TestSmartListSerializer#canRoundTripSerializableListsNaive": 'naive',
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
    "StateSetQuickcheck#removeAdd": 'remove',
    "X509ResourceCertificateParentChildValidatorTest#validParentChildSubResources": 'resources'
}


def obtain_stats_directories(results_directory: str) -> list[str]:
    directory_tree = [x for x in os.walk(results_directory)] # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]

def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> dict[str, str]:
    valid_directories = []
    project_string = project_name if project_name != "convex" else project_name + "-core"  # edge case
    if "mph-table-fixed" in stats_directories[0]:  # edge case
        project_string = "mph-table-fixed"
    elif "mph-table-naive" in stats_directories[0]:
        project_string = "mph-table-naive"
    elif "rpki-commons-fixed" in stats_directories[0]:
        project_string = "rpki-commons-fixed"
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
        directory_stats = retrieve_time_elapsed(project_name=project_name, directory_path=directory_path, valid_htmls=valid_htmls)
        project_iteration = project_name + " - " + str(iteration)
        final_stats[project_iteration] = directory_stats
        iteration += 1
    return final_stats


def retrieve_time_elapsed(project_name: str, directory_path: str, valid_htmls: list[str]) -> dict[str, str]:
    times_elapsed_dict = {}
    for html_file in valid_htmls:
        property_name = html_file.replace(".html", "")
        if property_name not in propertyShortNames:
            continue
        property_short_name = propertyShortNames[property_name]
        if property_short_name == 'list*' and project_name == 'mph-table-fixed':
            property_short_name = 'list'
        elif property_short_name == 'list' and project_name == 'mph-table-fixed':
            continue
        elif property_short_name == 'list*':
            print(project_name)
            continue
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
            if property_array is None:
                property_dict[prop] = []
                property_array = property_dict.get(prop)
            property_array.append(time)

    # generate mean, standard deviation and populate our final object
    property_stats_dict = {}
    for key, val in property_dict.items():
        np_array = np.array(val)
        mean = '{:.2f}'.format(round(np_array.mean(), 2))
        standard_dev = '{:.2f}'.format(round(np_array.std(), 2))
        property_stats_dict[key] = str(mean) + " \u00B1 " + str(standard_dev)
    return property_stats_dict


def generate_project_report(project_name: str, final_stats: dict[str, str], final_fixed_stats: dict[str, str]) -> dict[str, dict]:
    final_report_dict = {project_name: final_stats, project_name + "-fixed": final_fixed_stats}
    return final_report_dict


def generate_mph_project_df(final_stats: dict[str, str], final_fixed_stats: dict[str, str], final_naive_stats: dict[str, str], row_count: int) -> (pd.DataFrame(), int):
    vanilla_df = pd.DataFrame()
    vanilla_df['Property'] = [key for key in final_stats.keys()]
    vanilla_df['Vanilla'] = [val for val in final_stats.values()]

    improved_df = pd.DataFrame()
    improved_df['Property'] = [key for key in final_fixed_stats.keys()]
    improved_df['Improved'] = [val for val in final_fixed_stats.values()]

    naive_df = pd.DataFrame()
    naive_df['Property'] = ['naive']
    naive_df['Vanilla'] = [final_stats['list']]
    naive_df['Improved'] = [final_naive_stats['naive']]
  
    merged_df = pd.merge(vanilla_df, improved_df, how='outer', on='Property')
    merged_final_df = pd.concat([merged_df, naive_df]).reset_index()

    merged_final_df['N'] = pd.RangeIndex(start=row_count, stop=len(merged_final_df.index) + row_count)
    row_count += len(merged_final_df.index)
    final_df = merged_final_df[['N', 'Property', 'Vanilla', 'Improved']]
    return final_df, row_count

def generate_project_df(final_stats: dict[str, str], final_fixed_stats: dict[str, str], row_count: int) -> (pd.DataFrame(), int):
    vanilla_df = pd.DataFrame()
    vanilla_df['Property'] = [key for key in final_stats.keys()]
    vanilla_df['Vanilla'] = [val for val in final_stats.values()]

    improved_df = pd.DataFrame()
    improved_df['Property'] = [key for key in final_fixed_stats.keys()]
    improved_df['Improved'] = [val for val in final_fixed_stats.values()]

    merged_df = pd.merge(vanilla_df, improved_df, how='outer', on='Property')
    merged_df['N'] = pd.RangeIndex(start=row_count, stop=len(merged_df.index) + row_count)
    row_count += len(merged_df.index)
    final_df = merged_df[['N', 'Property', 'Vanilla', 'Improved']]
    return final_df, row_count


def main():
    final_dataset = {}
    row_count = 1
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

        if project_name == "mph-table":
            naive_project_name = "mph-table-naive"
            naive_results_directory = BASE_RESULT_DIR + naive_project_name + "/"
            naive_stats_directories = obtain_stats_directories(results_directory=naive_results_directory)
            evaluated_naive_runs = filter_for_recent_results(project_name=project_name, stats_directories=naive_stats_directories)
            naive_stats = evaluate_directories(project_name=naive_project_name, results_directory=naive_results_directory, directories=evaluated_naive_runs)
            # obtain mean/st dev
            final_stats = generate_report_stats(stat_values=raw_stats)
            final_fixed_stats = generate_report_stats(stat_values=fixed_raw_stats)
            final_naive_stats = generate_report_stats(stat_values=naive_stats)
            project_df, row_count = generate_mph_project_df(final_stats=final_stats, final_fixed_stats=final_fixed_stats, final_naive_stats=final_naive_stats, row_count=row_count)
            final_dataset[project_name] = project_df
        else:
            # obtain mean/st dev
            final_stats = generate_report_stats(stat_values=raw_stats)
            final_fixed_stats = generate_report_stats(stat_values=fixed_raw_stats)
            project_df, row_count = generate_project_df(final_stats=final_stats, final_fixed_stats=final_fixed_stats, row_count=row_count)
            final_dataset[project_name] = project_df


    with open(TEX_REPORT_NAME, 'w') as tf:
        df = pd.DataFrame()
        for project in PROJECTS:
            final_dataset[project]['_style'] = ''
            proj_mean_and_std = final_dataset[project][RAW_NAMES].copy()
            vanilla_mean = pd.DataFrame(proj_mean_and_std['Vanilla'].apply(lambda v: float(v.split(" \u00B1 ")[0]) if
                                                                " \u00B1 " in str(v) else np.nan)).reset_index()
            improved_mean = pd.DataFrame(proj_mean_and_std['Improved'].apply(lambda v: float(v.split(" \u00B1 ")[0]) if
                                                                " \u00B1 " in str(v) else np.nan)).reset_index()
            print(project)
            print(improved_mean)
            print(vanilla_mean)
            proj_stats = pd.merge(vanilla_mean, improved_mean, how='outer', on='index')[RAW_NAMES].reset_index()
            #print(proj_stats)
            final_dataset[project]['Overhead'] = proj_stats[['Improved']].values / proj_stats[['Vanilla']].values
            overhead_stats = final_dataset[project]['Overhead'].copy().reset_index()

            proj_mean = pd.merge(proj_stats, overhead_stats, how='outer', on='index')[CALC_NAMES].mean()
            proj_mean['_style'] = 'BOLD'
            proj_mean['N'] = ''
            proj_mean['Property'] = 'Average'
            final_dataset[project].loc['mean'] = proj_mean

            header = dict(zip(['N', 'Property', 'Vanilla', 'Improved', 'Overhead'], ['', '', '', '', '']))
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
                outTable += '\\hline' + "\n" + '\multicolumn{' + c + '}{c}{\\' + s[1].strip()[7:].strip().replace("-", "") + '}' + " \\\\\n" + '\\hline' + "\n"
            else:
                outTable += line.replace("nan", "-")

        tf.write(outTable)


if __name__ == "__main__":
    main()
