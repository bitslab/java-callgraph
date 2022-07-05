package gr.gousiosg.javacg.stat.coverage;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class CoverageStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageStatistics.class);
    private static final String SEPARATOR = "#############################";

    @Writeable
    private long edgeCount = 0;
    @Writeable
    private long nodesCovered = 0;
    @Writeable
    private long nodeCount = 0;
    @Writeable
    private int linesCovered = 0;
    @Writeable
    private int linesMissed = 0;
    @Writeable
    private int branchesCovered = 0;
    @Writeable
    private int branchesMissed = 0;

    /**
     * Quantifies the coverage quality of the graph
     *
     * @param graph the graph to quantify coverage quality for
     */
    private CoverageStatistics(Graph<ColoredNode, DefaultEdge> graph) {
        for (ColoredNode node : graph.vertexSet()) {
            if (node.isExcluded()) {
                continue;
            }

            this.linesCovered += node.getLinesCovered();
            this.linesMissed += node.getLinesMissed();
            this.branchesCovered += node.getBranchesCovered();
            this.branchesMissed += node.getBranchesMissed();
            this.edgeCount += graph.outDegreeOf(node);
            this.nodeCount++;

            if (node.covered()) {
                this.nodesCovered += 1;
            }
        }
    }

    public static void analyze(Graph<ColoredNode, DefaultEdge> graph, Optional<String> outputName) {
        CoverageStatistics statistics = new CoverageStatistics(graph);
        statistics.announce();
        if (outputName.isPresent()) {
            try {
                toCsv(statistics, outputName.get());
            } catch (Exception e) {
                LOGGER.error("Error writing " + outputName.get() + " to CSV!");
            }
        }
    }

    private static void toCsv(CoverageStatistics statistics, String fileName) throws Exception {
        if (statistics == null) return;
        FileWriter writer = new FileWriter(fileName);
        Arrays.stream(CoverageStatistics.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Writeable.class))
                .forEach(
                        f -> {
                            try {
                                writer.write(f.getName() + "," + f.get(statistics) + "\n");
                            } catch (IllegalAccessException | IOException e) {
                                e.printStackTrace();
                                LOGGER.error("Unable to write statistics to " + fileName);
                            }
                        });
        writer.close();
    }

    private void announce() {
        float nodeCoverage = ((float) this.nodesCovered) / this.nodeCount * 100;
        float lineCoverage = ((float) this.linesCovered) / (this.linesCovered + linesMissed) * 100;
        float branchCoverage =
                ((float) this.branchesCovered) / (this.branchesCovered + this.branchesMissed) * 100;

        nodeCoverage = (Float.isNaN(nodeCoverage)) ? 0 : nodeCoverage;
        lineCoverage = (Float.isNaN(lineCoverage)) ? 0 : lineCoverage;
        branchCoverage = (Float.isNaN(branchCoverage)) ? 0 : branchCoverage;

        LOGGER.info(SEPARATOR);
        LOGGER.info("Coverage Statistics:");
        LOGGER.info("Edge Count:            " + this.edgeCount);
        LOGGER.info("Node Count:            " + this.nodeCount);
        LOGGER.info("Nodes Covered:         " + this.nodesCovered);
        LOGGER.info("Lines Covered:         " + this.linesCovered);
        LOGGER.info("Lines Missed:          " + this.linesMissed);
        LOGGER.info("Branches Covered:      " + this.branchesCovered);
        LOGGER.info("Branches Missed:       " + this.branchesMissed);
        LOGGER.info("Method Coverage:       " + String.format("%.2f", nodeCoverage) + "%");
        LOGGER.info("Line Coverage:         " + String.format("%.2f", lineCoverage) + "%");
        LOGGER.info("Branch Coverage:       " + String.format("%.2f", branchCoverage) + "%");
        LOGGER.info(SEPARATOR);
    }
}
