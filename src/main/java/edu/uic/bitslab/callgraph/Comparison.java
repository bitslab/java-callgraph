package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.coverage.Report;
import gr.gousiosg.javacg.stat.support.RepoTool;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Comparison {
    private static final String PROJECT_NAME = "p";
    private static final String PROJECT_NAME_LONG = "project";
    private static final String OUTPUT_FILE_NAME = "o";
    private static final String OUTPUT_FILE_NAME_LONG = "output";
    private static final String ARTIFACT_TIMESTAMP = "t";
    private static final String ARTIFACT_TIMESTAMP_LONG = "timestamp";


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
                    int branchesCovered = Integer.MIN_VALUE;
                    int branchedMissed = Integer.MIN_VALUE;
                    int linesCovered = Integer.MIN_VALUE;
                    int linesMissed = Integer.MIN_VALUE;

                    for (Report.Package.Class.Method.Counter counter : details.getCounter()) {
                        if (counter.getType().equals("BRANCH")) {
                            branchesCovered = counter.getCovered();
                            branchedMissed = counter.getMissed();
                        } else if (counter.getType().equals("LINE")) {
                            linesCovered = counter.getCovered();
                            linesMissed = counter.getMissed();
                        }
                    }

                    r.addJaCoCo(branchesCovered, branchedMissed, linesCovered, linesMissed);

                    rpt.put(key, r);
                }
            );

            return rpt;
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        return null;
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
        String resultsDir = "artifacts/results/";
        String glob = project + "????-??-??T??_??_??.??????";
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

            timeStamp = latestPath.getFileName().toString().substring(project.length());
        }

        Comparison comparison = new Comparison();

        RepoTool rt = new RepoTool(project, timeStamp);
        List<Pair<String,?>> coverageFiles = rt.obtainCoverageFilesAndEntryPoints();

        for (Pair<String, ?> coverageFile : coverageFiles) {
            // need pruned graph ser file part of artifacts!
            String jacocoXMLFilename = coverageFile.first;
            String prunedGraphSerFile = coverageFile.first.substring(0, coverageFile.first.length()-4) + "-reachability.ser";

            Map<String, Row> rpt = comparison.pruneAndJaCoCo(jacocoXMLFilename, prunedGraphSerFile);


            String header = String.join(",",
                            "entryPoint", "method", "nodeColor",
                            "branchesCovered", "branchesMissed", "linesCovered", "linesMissed",
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
        }
    }

    static class Row {
        private final String entryPoint;
        private final String method;
        private String nodeColor = "";
        private int branchesCovered = Integer.MIN_VALUE;
        private int branchesMissed = Integer.MIN_VALUE;
        private int linesCovered = Integer.MIN_VALUE;
        private int linesMissed = Integer.MIN_VALUE;
        private boolean inJaCoCo = false;
        private boolean inPrunedGraph = false;

        Row(String entryPoint, String method) {
            this.entryPoint = entryPoint;
            this.method = method;
        }

        public void addJaCoCo(int branchesCovered, int branchesMissed, int linesCovered, int linesMissed) {
            this.branchesCovered = branchesCovered;
            this.branchesMissed = branchesMissed;
            this.linesCovered = linesCovered;
            this.linesMissed = linesMissed;
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
                    cntToStr(branchesCovered),
                    cntToStr(branchesMissed),
                    cntToStr(linesCovered),
                    cntToStr(linesMissed),
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
                Option.builder(ARTIFACT_TIMESTAMP)
                        .longOpt(ARTIFACT_TIMESTAMP_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify the artifact timestamp (defaults to latest run in artifacts)")
                        .required(false)
                        .build());


        return options;
    }

}
