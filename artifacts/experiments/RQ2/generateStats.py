import glob
import os
import subprocess

JAR_FILE = "target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar"

PROJECTS = ["convex", "jflex", "mph-table", "rpki-commons"]
TRIALS = [10, 50, 500, 1000]


def test_properties(project_name: str):
    project_graph = project_name + "_graph"
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                    project_name, "-f", project_graph])


def test_properties_with_trials(project_name: str, trials: int):
    project_name_with_trials = project_name + "-" + str(trials)
    project_graph = project_name_with_trials + "_graph"
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                    project_name_with_trials, "-f", project_graph])


def clear_output():
    files = glob.glob('output/*')
    for f in files:
        os.remove(f)


def main():
    for project in PROJECTS:
        test_properties(project_name=project)
        clear_output()
        for trial in TRIALS:
            test_properties_with_trials(project_name=project, trials=trial)
            clear_output()


if __name__ == "__main__":
    main()
