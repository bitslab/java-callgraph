import sys
import subprocess

JAR_FILE = "target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar"

PROJECTS = ["mph-table"]
TRIALS = [10]


def test_properties(project_name: str, trials: int):
    project_name_with_trials = project_name + "-" + str(trials)
    project_graph = project_name_with_trials + "_graph"
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                    project_name_with_trials, "-f", project_graph])


def main():
    for project in PROJECTS:
        for trial in TRIALS:
            test_properties(project_name=project, trials=trial)

if __name__ == "__main__":
    main()
