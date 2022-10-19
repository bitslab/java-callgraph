package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.support.RepoTool;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(Comparison.class);

    public String pruneAndJaCoCo(String jacocoXMLFilename, String prunedGraphSerFile) {
        StringBuilder s = new StringBuilder();

        try {
            Graph<ColoredNode, DefaultEdge> prunedGraph;

            try (ObjectInput ois = new ObjectInputStream(new FileInputStream(prunedGraphSerFile))) {
                prunedGraph = returnGraph(ois.readObject());
            }

            if (prunedGraph == null) {
                throw new Exception("pruned graph is null");
            }

            JacocoCoverage jacocoCoverage = new JacocoCoverage(jacocoXMLFilename);
            String entryPoint = prunedGraph
                    .vertexSet()
                    .stream()
                    .filter(v -> prunedGraph.inDegreeOf(v) == 0)
                    .map(ColoredNode::getLabel)
                    .findFirst()
                    .orElse(null);

            s.append("entryPoint,method,branchesCovered,branchesMissed,linesCovered,linesMissed,inJaCoCo,nodeColor\n");

            for (ColoredNode v : prunedGraph.vertexSet()) {
                String methodSignature = v.getLabel();

                s.append("\"").append(entryPoint).append("\",")
                    .append("\"").append(methodSignature).append("\",")
                    .append(v.getBranchesCovered()).append(",")
                    .append(v.getBranchesMissed()).append(",")
                    .append(v.getLinesCovered()).append(",")
                    .append(v.getLinesMissed()).append(",")
                    .append((jacocoCoverage.containsMethod(methodSignature) ? "1" : "0")).append(",")
                    .append(v.getColor())
                    .append("\n");
            }

            return s.toString();
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

    public static void main(String[] args) throws Exception {
        String project = args[0];
        String timeStamp;

        if (args.length == 2) {
            timeStamp = args[1];
        } else {
            // get last one in results
            String resultsDir = "artifacts/results/";
            String glob = project + "????-??-??T??_??_??.??????";
            Path latestPath = null;

            for (Path path : Files.newDirectoryStream(Path.of(resultsDir), glob)) {
                if (latestPath == null || path.compareTo(latestPath) > 0) {
                    latestPath = path;
                }
            }

            if (latestPath == null) {
                LOGGER.error("No result directory found for " + project + ".");
                System.exit(1);
            }

            timeStamp = latestPath.getFileName().toString().substring(project.length());
        }

        Comparison comparison = new Comparison();

        RepoTool rt = new RepoTool(project, timeStamp);
        List<Pair<String,?>> coverageFiles = rt.obtainCoverageFilesAndEntryPoints();

        for (Pair<String, ?> coverageFile : coverageFiles) {
            // need pruned graph ser file part of artifacts!
            String jacocoXMLFilename = coverageFile.first;
            String prunedGraphSerFile = coverageFile.first.substring(0, coverageFile.first.length()-4) + "-reachability.ser";

            String result = comparison.pruneAndJaCoCo(jacocoXMLFilename, prunedGraphSerFile);
            System.out.println(result);

        }

    }
}
