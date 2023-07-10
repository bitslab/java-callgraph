package edu.uic.bitslab.callgraph.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Reachability {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reachability.class);

    /**
     * Computes the reachability subgraph from a parent graph, entrypoint, and an optional depth to
     * search
     *
     * @param graph             the parent {@link Graph}
     * @param entrypoint        the root node of the reachability subgraph
     * @param maybeMaximumDepth the depth to traverse (e.g., all nodes reachable within N steps from
     *                          the root)
     * @return a subgraph containing all reachable nodes as described
     */
    public static Graph<ColoredNode, DefaultEdge> compute(
            Graph<String, DefaultEdge> graph, String entrypoint, Optional<Integer> maybeMaximumDepth) {

        if (!graph.containsVertex(entrypoint)) {
            LOGGER.error("---> " + entrypoint + "<---");
            LOGGER.error("The graph doesn't contain the vertex specified as the entry point!");
            throw new InputMismatchException("graph doesn't contain vertex " + entrypoint);
        }

        if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < 0)) {
            LOGGER.error("Depth " + maybeMaximumDepth.get() + " must be greater than 0!");
            System.exit(1);
        }

        LOGGER.info("Starting reachability at entry point: " + entrypoint);
        maybeMaximumDepth.ifPresent(d -> LOGGER.info("Traversing to depth " + d));

        Graph<ColoredNode, DefaultEdge> subgraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        int currentDepth = 0;

        Deque<String> reachable = new ArrayDeque<>();
        reachable.push(entrypoint);

        Map<String, ColoredNode> subgraphNodes = new HashMap<>();

        // Initialize
        {
            ColoredNode node = new ColoredNode(entrypoint);
            subgraphNodes.put(entrypoint, node);
            subgraph.addVertex(node);
            node.markEntryPoint();
        }

        while (!reachable.isEmpty()) {

            /* Stop once we've surpassed maximum depth */
            if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < currentDepth)) {
                break;
            }

            /* Visit reachable node */
            String source = reachable.pop();
            ColoredNode sourceNode =
                    subgraphNodes.containsKey(source) ? subgraphNodes.get(source) : new ColoredNode(source);

            /* Keep track of who we've visited */
            if (!subgraphNodes.containsKey(source))
                throw new AssertionError();

            Set<DefaultEdge> edges = graph.outgoingEdgesOf(source);

            graph
                    .outgoingEdgesOf(source)
                    .stream()
                    .forEach(
                            edge -> {
                                String target = graph.getEdgeTarget(edge);
                                ColoredNode targetNode = subgraphNodes.get(target);

                                if (targetNode == null) {
                                    targetNode = new ColoredNode(target);
                                    subgraphNodes.put(target, targetNode);
                                    subgraph.addVertex(targetNode);
                                    reachable.add(target);
                                }

                                subgraph.addEdge(sourceNode, targetNode);
                            });

            currentDepth++;
        }

        Set<DefaultEdge> bi = graph.incomingEdgesOf("com.indeed.mph.SmartSerializer.write - RoundTripHelpers.java:22");
        Set<DefaultEdge> bo = graph.outgoingEdgesOf("com.indeed.mph.SmartSerializer.write - RoundTripHelpers.java:22");
        ColoredNode n = subgraphNodes.get("com.indeed.mph.SmartSerializer.write - RoundTripHelpers.java:22");
        Set<DefaultEdge> ai = subgraph.incomingEdgesOf(n);
        Set<DefaultEdge> ao = subgraph.outgoingEdgesOf(n);

        return subgraph;
    }
}
