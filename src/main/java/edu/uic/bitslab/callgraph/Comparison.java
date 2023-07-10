package edu.uic.bitslab.callgraph;

import com.opencsv.CSVReader;
import edu.uic.bitslab.callgraph.config.SUTConfig;
import gr.gousiosg.javacg.dyn.Pair;
import edu.uic.bitslab.callgraph.graph.ColoredNode;
import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.coverage.Report;
import edu.uic.bitslab.callgraph.support.RepoTool;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Comparison {
    private static final String PROJECT_NAME = "p";
    private static final String PROJECT_NAME_LONG = "project";
    private static final String OUTPUT_FILE_NAME = "o";
    private static final String OUTPUT_FILE_NAME_LONG = "output";
    private static final String ARTIFACT_TIMESTAMP = "t";
    private static final String ARTIFACT_TIMESTAMP_LONG = "timestamp";
    private static final String OUTPUT_PATH_FILE_NAME = "c";
    private static final String OUTPUT_PATH_FILE_NAME_LONG = "outpathcount";


    private static final Logger LOGGER = LoggerFactory.getLogger(Comparison.class);

    private static String getKey(String entryPoint, String method) {
        return entryPoint + ":" + method;
    }


    public Map<String, Row> pruneAndJaCoCo(String jacocoXMLFilename, String prunedGraphSerFile) {
        Map<String, Row> rpt = new HashMap<>();

        try {
            Graph<ColoredNode, DefaultEdge> prunedGraph;

            try (ObjectInput ois = new ObjectInputStream(new FileInputStream(prunedGraphSerFile))) {
                prunedGraph = returnGraph(ois.readObject());
            }

            if (prunedGraph == null) {
                throw new Exception("pruned graph is null");
            }

            String entryPoint = prunedGraph
                    .vertexSet()
                    .stream()
                    .filter(v -> prunedGraph.inDegreeOf(v) == 0)
                    .map(ColoredNode::getLabel)
                    .findFirst()
                    .orElse(null);

            for (ColoredNode v : prunedGraph.vertexSet()) {
                String method = v.getLabel();
                String key = getKey(entryPoint, method);

                Row r = rpt.getOrDefault(key, new Row(entryPoint, method));
                r.addPruned(v.getColor());
                rpt.put(key, r);
            }

            JacocoCoverage jacocoCoverage = new JacocoCoverage(jacocoXMLFilename);
            Map<String, Report.Package.Class.Method> methodCoverage = jacocoCoverage.getMethodCoverage();
            methodCoverage.forEach(
                (method, details) -> {
                    String key = getKey(entryPoint, method);
                    Row r = rpt.getOrDefault(key, new Row(entryPoint, method));


                    int instructionCovered = Integer.MIN_VALUE;
                    int instructionMissed = Integer.MIN_VALUE;
                    int branchesCovered = Integer.MIN_VALUE;
                    int branchedMissed = Integer.MIN_VALUE;
                    int linesCovered = Integer.MIN_VALUE;
                    int linesMissed = Integer.MIN_VALUE;
                    int complexityCovered = Integer.MIN_VALUE;
                    int complexityMissed = Integer.MIN_VALUE;
                    int methodCovered = Integer.MIN_VALUE;
                    int methodMissed = Integer.MIN_VALUE;


                    for (Report.Package.Class.Method.Counter counter : details.getCounter()) {
                        switch (counter.getType()) {
                            case "INSTRUCTION":
                                instructionCovered = counter.getCovered();
                                instructionMissed = counter.getMissed();
                                break;
                            case "BRANCH":
                                branchesCovered = counter.getCovered();
                                branchedMissed = counter.getMissed();
                                break;
                            case "LINE":
                                linesCovered = counter.getCovered();
                                linesMissed = counter.getMissed();
                                break;
                            case "COMPLEXITY":
                                complexityCovered = counter.getCovered();
                                complexityMissed = counter.getMissed();
                                break;
                            case "METHOD":
                                methodCovered = counter.getCovered();
                                methodMissed = counter.getMissed();
                                break;
                        }
                    }

                    r.addJaCoCo(instructionCovered, instructionMissed, branchesCovered, branchedMissed, linesCovered, linesMissed, complexityCovered, complexityMissed, methodCovered, methodMissed);

                    rpt.put(key, r);
                }
            );

            return rpt;
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        return null;
    }

    public Map<String, List<Integer>> pathCounts(List<String> pathFiles) throws Exception {
        Map<String, List<Integer>> paths = new HashMap<>();

        for (String pathFile : pathFiles) {
            if (!Files.exists(Path.of(pathFile))) {
                continue;
            }

            List<Integer> pathsList = new ArrayList<>();

            try (CSVReader csvReader = new CSVReader(new FileReader(pathFile))) {
                String[] values;
                String entryPoint = null;

                while ((values = csvReader.readNext()) != null) {
                    // skip lines with less that expected values (expect 3+ columns)
                    if (values.length < 3) continue;

                    String nextEntryPoint = values[2];

                    // skip empty entrypoints
                    if (nextEntryPoint.isEmpty()) continue;

                    // we don't have an entryPoint so set it
                    if (entryPoint == null) entryPoint = nextEntryPoint;

                    // put in some guard rails
                    if (!entryPoint.equals(nextEntryPoint)) {
                        throw new Exception("Entrypoint should be same within each file");
                    }
                    
                    int pathLength = values.length - 2;

                    pathsList.add(pathLength);
                }

                if (entryPoint != null) {
                    paths.put(
                            entryPoint,
                            pathsList.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())
                    );
                }
            }
        }

        return paths;
    }

    @SuppressWarnings("unchecked")
    private Graph<ColoredNode, DefaultEdge> returnGraph(Object o) {
        if (o instanceof Graph) {
            return (Graph<ColoredNode, DefaultEdge>) o;
        }

        LOGGER.error("Expected instanceof Graph, but received " + o.getClass().getName() + " instead.");
        return null;
    }

    private static Path getLatestResultPath(String project) throws IOException {
        SUTConfig sutConfig = SUTConfig.fromProjectName(project);
        String resultsDir = "artifacts/results/" + project;
        String glob = (sutConfig.subProject.equals("") ? project : sutConfig.subProject) + "????-??-??T??_??_??.??????";
        Path latestPath = null;

        for (Path path : Files.newDirectoryStream(Path.of(resultsDir), glob)) {
            if (latestPath == null || path.compareTo(latestPath) > 0) {
                latestPath = path;
            }
        }

        return latestPath;
    }



    public static void main(String[] args) throws Exception {
        String project = null;
        String timeStamp = null;
        String outputFile = null;
        String outputPathFile = null;

        List<String> filePaths = new ArrayList<>();

        /* Setup cmdline argument parsing */
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption(PROJECT_NAME)) {
                project = cmd.getOptionValue(PROJECT_NAME);
            }

            if (cmd.hasOption(ARTIFACT_TIMESTAMP)) {
                timeStamp = cmd.getOptionValue(ARTIFACT_TIMESTAMP);
            }

            if (cmd.hasOption(OUTPUT_FILE_NAME)) {
                outputFile = cmd.getOptionValue(OUTPUT_FILE_NAME);
            }

            if (cmd.hasOption(OUTPUT_PATH_FILE_NAME)) {
                outputPathFile = cmd.getOptionValue(OUTPUT_PATH_FILE_NAME);
            }
        } catch(ParseException pe) {
            LOGGER.error("Error parsing command-line arguments: " + pe.getMessage());
            LOGGER.error("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Build comparison for RQ1", options);
            System.exit(1);
        }

        if (timeStamp == null) {
            // get last one in results

            Path latestPath = getLatestResultPath(project);

            if (latestPath == null) {
                LOGGER.error("No result directory found for " + project + ".");
                System.exit(1);
            }

            assert project != null;

            String dirPath = latestPath.getFileName().toString();
            timeStamp = dirPath.substring(dirPath.length() - 26);
        }

        Comparison comparison = new Comparison();

        RepoTool rt = new RepoTool(project, timeStamp);
        List<Pair<String,?>> coverageFiles = rt.obtainCoverageFilesAndEntryPoints();
        Map<String, Row> rpt = new HashMap<>();

        for (Pair<String, ?> coverageFile : coverageFiles) {
            // need pruned graph ser file part of artifacts!
            String jacocoXMLFilename = coverageFile.first;
            String prunedGraphSerFile = coverageFile.first.substring(0, coverageFile.first.length()-4) + "-reachability.ser";
            filePaths.add(coverageFile.first.substring(0, coverageFile.first.length()-4) + "-paths.csv");

            rpt.putAll(comparison.pruneAndJaCoCo(jacocoXMLFilename, prunedGraphSerFile));
        }

        String header = String.join(",",
                "entryPoint", "method", "nodeColor",
                "instructionCovered", "instructionMissed",
                "branchesCovered", "branchesMissed",
                "linesCovered", "linesMissed",
                "complexityCovered","complexityMissed",
                "methodCovered","methodMissed",
                "inJaCoCo", "inPrunedGraph"
        );

        if (outputFile == null) {
            System.out.println(header);
            rpt.forEach((k, r) -> System.out.println(r));
        } else {
            try(FileWriter writer = new FileWriter(outputFile)) {
                writer.write(header + "\n");

                for (Row r : rpt.values()) {
                    writer.write(r + "\n");
                }
            }
        }

        // build path counts
        String headerPath = String.join(",", "entryPoint", "First", "Second", "Third");

        Map<String, List<Integer>> paths = comparison.pathCounts(filePaths);

        if (outputPathFile == null) {
            System.out.println(headerPath);
            paths.forEach(
                    (entryPoint, pathCounts) -> System.out.println(
                        "\"" + entryPoint + "\"," +
                        pathCounts.stream().limit(3).map(Object::toString).collect(Collectors.joining(","))
                    )
                );
        } else {
            try(FileWriter writer = new FileWriter(outputPathFile)) {
                writer.write(headerPath + "\n");

                for (Map.Entry<String, List<Integer>> entry : paths.entrySet()) {
                    String entryPoint = entry.getKey();
                    List<Integer> pathCounts = entry.getValue();

                    writer.write(
                            "\"" +
                                    entryPoint +
                                    "\"," +
                                    pathCounts.stream()
                                            .limit(3)
                                            .map(Object::toString)
                                            .collect(Collectors.joining(",")) +
                                    "\n"
                    );
                }
            }
        }
    }

    static class Row {
        private final String entryPoint;
        private final String method;
        private String nodeColor = "";

        private int instructionCovered = Integer.MIN_VALUE;
        private int instructionMissed = Integer.MIN_VALUE;
        private int branchesCovered = Integer.MIN_VALUE;
        private int branchesMissed = Integer.MIN_VALUE;
        private int linesCovered = Integer.MIN_VALUE;
        private int linesMissed = Integer.MIN_VALUE;
        private int complexityCovered = Integer.MIN_VALUE;
        private int complexityMissed = Integer.MIN_VALUE;
        private int methodCovered = Integer.MIN_VALUE;
        private int methodMissed = Integer.MIN_VALUE;

        private boolean inJaCoCo = false;
        private boolean inPrunedGraph = false;

        Row(String entryPoint, String method) {
            this.entryPoint = entryPoint;
            this.method = method;
        }

        public void addJaCoCo(int instructionCovered, int instructionMissed, int branchesCovered, int branchesMissed, int linesCovered, int linesMissed, int complexityCovered, int complexityMissed, int methodCovered, int methodMissed) {
            this.instructionCovered = instructionCovered;
            this.instructionMissed = instructionMissed;
            this.branchesCovered = branchesCovered;
            this.branchesMissed = branchesMissed;
            this.linesCovered = linesCovered;
            this.linesMissed = linesMissed;
            this.complexityCovered = complexityCovered;
            this.complexityMissed = complexityMissed;
            this.methodCovered = methodCovered;
            this.methodMissed = methodMissed;
            this.inJaCoCo = true;
        }

        public void addPruned(String nodeColor) {
            this.nodeColor = nodeColor;
            this.inPrunedGraph = true;
        }

        @Override
        public String toString() {
            return String.join(",",
                    "\"" + entryPoint + "\"",
                    "\"" + method + "\"",
                    "\"" + nodeColor + "\"",

                    // stuff here
                    cntToStr(instructionCovered),
                    cntToStr(instructionMissed),
                    cntToStr(branchesCovered),
                    cntToStr(branchesMissed),
                    cntToStr(linesCovered),
                    cntToStr(linesMissed),
                    cntToStr(complexityCovered),
                    cntToStr(complexityMissed),
                    cntToStr(methodCovered),
                    cntToStr(methodMissed),
                    inJaCoCo ? "Y" : "N",
                    inPrunedGraph ? "Y" : "N"
            );
        }

        private String cntToStr(int cnt) {
            return cnt == Integer.MIN_VALUE ? "UNK" : String.valueOf(cnt);
        }
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder(PROJECT_NAME)
                        .longOpt(PROJECT_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify the project name")
                        .required(true)
                        .build());

        options.addOption(
                Option.builder(OUTPUT_FILE_NAME)
                        .longOpt(OUTPUT_FILE_NAME_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify the output filename (default to stdout)")
                        .required(false)
                        .build());

        options.addOption(
                Option.builder(OUTPUT_PATH_FILE_NAME)
                        .longOpt(OUTPUT_PATH_FILE_NAME_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify the path count output filename (default to stdout)")
                        .required(false)
                        .build());

        options.addOption(
                Option.builder(ARTIFACT_TIMESTAMP)
                        .longOpt(ARTIFACT_TIMESTAMP_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify the artifact timestamp (defaults to latest run in artifacts)")
                        .required(false)
                        .build());


        return options;
    }

}
