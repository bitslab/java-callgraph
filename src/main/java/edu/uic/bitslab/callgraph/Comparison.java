package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(Comparison.class);

    public void pruneToJaCoCo(String jacocoXMLFilename, String prunedGraphSerFile, List<String> foundMethods) {
        try {
            Graph<ColoredNode, DefaultEdge> prunedGraph;

            try (ObjectInput ois = new ObjectInputStream(new FileInputStream(prunedGraphSerFile))) {
                prunedGraph = returnGraph(ois.readObject());
            }

            if (prunedGraph == null) {
                throw new Exception("pruned graph is null");
            }

            JacocoCoverage jacocoCoverage = new JacocoCoverage(jacocoXMLFilename);

            for (ColoredNode v : prunedGraph.vertexSet()) {
                String methodSignature = v.getLabel();
                if (jacocoCoverage.containsMethod(methodSignature)) {
                    foundMethods.add(methodSignature);
                }
            }
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Graph<ColoredNode, DefaultEdge> returnGraph(Object o) {
        if (o instanceof Graph) {
            return (Graph<ColoredNode, DefaultEdge>) o;
        }

        LOGGER.error("Expected instanceof Graph, but received " + o.getClass().getName() + " instead.");
        return null;
    }

    public static void main(String[] args) {
        Comparison comparison = new Comparison();

        String jacocoXMLFilename = args[0];
        String prunedGraphSerFile = args[1];
        ArrayList<String> foundMethods = new ArrayList<>();
        comparison.pruneToJaCoCo(jacocoXMLFilename, prunedGraphSerFile, foundMethods);

        foundMethods.stream().filter(m -> !m.contains(".<init>(")).forEach(System.out::println);
    }
}
