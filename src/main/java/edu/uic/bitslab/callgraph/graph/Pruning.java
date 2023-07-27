package edu.uic.bitslab.callgraph.graph;

import edu.uic.bitslab.callgraph.config.ManualOptions;
import edu.uic.bitslab.callgraph.config.SUTConfig;
import edu.uic.bitslab.callgraph.pruning.PruneMethods;
import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;
import edu.uic.bitslab.callgraph.support.JarMetadata;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static edu.uic.bitslab.callgraph.graph.Utilities.nodeMap;
import static java.util.function.Predicate.not;

public class Pruning {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pruning.class);

    public static void pruneOriginalGraph(String entryPoint, StaticCallgraph callgraph, JacocoCoverage coverage, SUTConfig sutConfig) {
        pruneOriginalGraph(entryPoint, callgraph, coverage, sutConfig.packages, sutConfig.virtualIncludeConcrete);
    }

    public static void pruneOriginalGraph(String entryPoint, StaticCallgraph callgraph, JacocoCoverage coverage, ManualOptions manualOptions) {
        // get manualOptions - convert from list to set
        HashMap<String, Set<String>> asSetVIC = new HashMap<>();
        manualOptions.virtualIncludeConcrete.forEach((s, l) -> asSetVIC.put(s, new HashSet<>(l)));

        pruneOriginalGraph(entryPoint, callgraph, coverage, new HashSet<>(manualOptions.packages), asSetVIC);
    }

    /**
     * Wrapper method to call {@link Pruning} methods
     *
     * @param callgraph the graph
     * @param coverage  the coverage
     */
    public static void pruneOriginalGraph(String entryPoint, StaticCallgraph callgraph, JacocoCoverage coverage, Set<String> packages, Map<String, Set<String>> virtualIncludeConcrete) {
        markConcreteBridgeTargets(callgraph.graph, callgraph.metadata);
        pruneBridgeMethods(callgraph.graph, callgraph.metadata);
        new PruneMethods(entryPoint, callgraph, coverage, packages, virtualIncludeConcrete).prune();

        if (packages.size() > 0) {
            trimLibraryCalls(entryPoint, callgraph, packages);
        }

        // prune test methods
//        pruneMethodsFromTests(entryPoint, callgraph);

//        pruneConcreteMethods(callgraph.graph, callgraph.metadata, coverage);

    }

    public static void pruneReachabilityGraph(Graph<ColoredNode, DefaultEdge> reachability, JarMetadata metadata, JacocoCoverage coverage) {
        pruneMethodsFromTestsThatAreReachable(reachability, metadata, coverage);
    }

    private static void trimLibraryCalls(String entryPoint, StaticCallgraph callgraph, Set<String> paks) {
        LinkedList<String> nodes = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        nodes.add(entryPoint);

        Set<String> toRemove = new HashSet<>();

        while (!nodes.isEmpty()) {
            String node = nodes.removeFirst();
            seen.add(node);

            boolean keep = paks.stream().map(node::startsWith).reduce(false, Boolean::logicalOr);

            if (!keep && callgraph.graph.outgoingEdgesOf(node).isEmpty())
                toRemove.add(node);

            callgraph.graph.outgoingEdgesOf(node).stream()
                    .map(callgraph.graph::getEdgeTarget)
                    .filter(not(seen::contains))
                    .forEach(nodes::addLast);
        }

        toRemove.forEach(n -> callgraph.graph.removeVertex(n));
    }

    /**
     * Remove all bridge / synthetic methods that were created during type erasure See
     * <a href="https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html">...</a> for more information.
     *
     * @param graph    the graph
     * @param metadata the metadata of the graph
     */
    private static void pruneBridgeMethods(Graph<String, DefaultEdge> graph, JarMetadata metadata) {
        metadata
                .getBridgeMethods()
                .forEach(
                        bridgeNode -> {
                            /* Fetch the bridge method and make sure it has exactly one outgoing edge */
                            Optional<DefaultEdge> maybeEdge =
                                    graph.outgoingEdgesOf(bridgeNode).stream().findFirst();

                            if (graph.outDegreeOf(bridgeNode) != 1 || maybeEdge.isEmpty()) {
                                /* announce the violator */
                                LOGGER.error(
                                        "Found a bridge method that doesn't have exactly 1 outgoing edge: "
                                                + bridgeNode
                                                + " : "
                                                + graph.outDegreeOf(bridgeNode));
                                /* announce the violator's connections */
                                graph
                                        .outgoingEdgesOf(bridgeNode)
                                        .forEach(
                                                e -> LOGGER.error(
                                                        "\t" + graph.getEdgeSource(e) + " -> " + graph.getEdgeTarget(e)));
                                System.exit(1);
                            }

                            /* Fetch the bridge method's target */
                            String bridgeTarget = graph.getEdgeTarget(maybeEdge.get());

                            /* Redirect all edges from the bridge method to its target */
                            graph
                                    .incomingEdgesOf(bridgeNode)
                                    .forEach(
                                            edge -> {
                                                String sourceNode = graph.getEdgeSource(edge);
                                                graph.addEdge(sourceNode, bridgeTarget);
                                            });

                            /* Remove the bridge method from the graph */
                            graph.removeVertex(bridgeNode);
                        });
    }

    /**
     * Remove all unused concrete method calls that are present in the graph
     *
     * <p>This technique helps us reduce the over-approximation incurred by method expansion.
     *
     * @param graph    the graph
     * @param metadata the metadata of the graph
     */
    private static void pruneConcreteMethods(
            Graph<String, DefaultEdge> graph, JarMetadata metadata, JacocoCoverage coverage) {
        metadata.getConcreteMethods().stream()
                .filter(concreteMethod -> !coverage.hasNonzeroCoverage(concreteMethod))
                .filter(m -> !m.contains("List.add"))
                .filter(m -> !m.contains("com.indeed"))
                .forEach(graph::removeVertex);
    }

    /**
     * Mark the target node of every concrete bridge method as concrete
     *
     * <p>If a bridge is concrete, then the bridge target should also be concrete
     *
     * @param graph    the graph
     * @param metadata the metadata of the graph
     */
    private static void markConcreteBridgeTargets(
            Graph<String, DefaultEdge> graph, JarMetadata metadata) {
        metadata.getBridgeMethods().stream()
                .filter(metadata::containsConcreteMethod)
                .map(graph::outgoingEdgesOf)
                .flatMap(Set::stream)
                .map(graph::getEdgeTarget)
                .forEach(metadata::addConcreteMethod);
    }

    /**
     * Prunes methods that are only called by tests
     * <p>
     * For example, a test method may call assertEquals. We should remove assertEquals from the graph.
     *
     * @param entryPoint entrypoint
     * @param callgraph callgraph
     */
    private static void pruneMethodsFromTests(String entryPoint, StaticCallgraph callgraph) {
        // prune the testMethods
        callgraph.metadata.testMethods.stream()
                .filter(s -> !s.equals(entryPoint))
                .filter(callgraph.graph::containsVertex)
                .forEach( s -> {
                    Set<DefaultEdge> incomingEdges = callgraph.graph.incomingEdgesOf(s);
                    Set<DefaultEdge> outgoingEdges = callgraph.graph.outgoingEdgesOf(s);

                    incomingEdges.forEach( incomingEdge -> {
                        String sourceVertex = callgraph.graph.getEdgeSource(incomingEdge);

                        outgoingEdges.forEach( outgoingEdge -> {
                            String targetVertex = callgraph.graph.getEdgeTarget(outgoingEdge);

                            callgraph.graph.addEdge(sourceVertex, targetVertex);
                        });
                    });

                    callgraph.graph.removeVertex(s);
                });
    }

    /**
     * Prunes callsites that are part of the test method jar.
     *
     * @param entryPoint entryPoint
     * @param callgraph callgraph
     */
    private static void pruneCallSitesFromTests(String entryPoint, StaticCallgraph callgraph) {
        // prune the test callsites
        callgraph.metadata.callSites.stream()
                .filter(callgraph.graph::containsVertex)
                .filter( a -> false )
                .forEach( s -> {
                    Set<DefaultEdge> incomingEdges = callgraph.graph.incomingEdgesOf(s);
                    Set<DefaultEdge> outgoingEdges = callgraph.graph.outgoingEdgesOf(s);

                    incomingEdges.forEach( incomingEdge -> {
                        String sourceVertex = callgraph.graph.getEdgeSource(incomingEdge);

                        outgoingEdges.forEach( outgoingEdge -> {
                            String targetVertex = callgraph.graph.getEdgeTarget(outgoingEdge);

                            callgraph.graph.addEdge(sourceVertex, targetVertex);
                        });
                    });

                    callgraph.graph.removeVertex(s);
                });
    }

    /**
     * Prunes methods that are only called by tests that are in the reachability graph
     * *  @param graph the graph
     *
     * @param metadata the metadata of the graph
     */
    private static void pruneMethodsFromTestsThatAreReachable(Graph<ColoredNode, DefaultEdge> graph, JarMetadata metadata, JacocoCoverage coverage) {
        Map<String, ColoredNode> nodeMap = nodeMap(graph.vertexSet());

        var testTargetNodes = metadata.testMethods.stream()
                .filter(nodeMap::containsKey)
                .map(target -> graph.outgoingEdgesOf(nodeMap.get(target)))
                .flatMap(Collection::stream)
                .map(graph::getEdgeTarget)
                .collect(Collectors.toSet());

        var targetsToRemove = testTargetNodes.stream()
                .filter(graph::containsVertex)
                .filter(targetNode -> {

                    if (targetNode.covered()) {
                        return false;
                    }

                    if (coverage.containsMethod(targetNode.getLabel())) {
                        return false;
                    }

                    if (metadata.testMethods.contains(targetNode.getLabel())) {
                        return false;
                    }

                    for (var e : graph.incomingEdgesOf(targetNode)) {
                        if (!metadata.testMethods.contains(graph.getEdgeSource(e).getLabel())) {
                            return false;
                        }
                    }

                    return true;
                })
                .filter(targetNode -> !metadata.testMethods.contains(targetNode.getLabel()))
                .collect(Collectors.toSet());

        targetsToRemove.forEach(graph::removeVertex);
    }
}
