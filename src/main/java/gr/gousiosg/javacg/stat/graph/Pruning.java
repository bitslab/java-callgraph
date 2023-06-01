package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.support.JarMetadata;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gr.gousiosg.javacg.stat.graph.Utilities.nodeMap;

public class Pruning {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pruning.class);

    /**
     * Wrapper method to call {@link Pruning} methods
     *
     * @param callgraph the graph
     * @param coverage  the coverage
     */
    public static void pruneOriginalGraph(StaticCallgraph callgraph, JacocoCoverage coverage) {
        markConcreteBridgeTargets(callgraph.graph, callgraph.metadata);
        pruneBridgeMethods(callgraph.graph, callgraph.metadata);
        pruneConcreteMethods(callgraph.graph, callgraph.metadata, coverage);
//        pruneMethodsFromTests(callgraph.graph, callgraph.metadata, coverage);
    }

    public static void pruneReachabilityGraph(Graph<ColoredNode, DefaultEdge> reachability, JarMetadata metadata, JacocoCoverage coverage) {
        pruneMethodsFromTestsThatAreReachable(reachability, metadata, coverage);
    }

    /**
     * Remove all bridge / synthetic methods that were created during type erasure See
     * https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html for more information.
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
                                                e -> {
                                                    LOGGER.error(
                                                            "\t" + graph.getEdgeSource(e) + " -> " + graph.getEdgeTarget(e));
                                                });
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
     * @param graph    the graph
     * @param metadata the metadata of the graph
     */
    private static void pruneMethodsFromTests(Graph<String, DefaultEdge> graph, JarMetadata metadata, JacocoCoverage coverage) {
        var testTargetNodes = metadata.testMethods.stream()
                .filter(graph::containsVertex)
                .map(graph::outgoingEdgesOf)
                .flatMap(Collection::stream)
                .map(graph::getEdgeTarget)
                .collect(Collectors.toSet());

        var targetsToRemove = testTargetNodes.stream()
                .filter(graph::containsVertex)
                .filter(target -> {
                    if (coverage.containsMethod(target)) {
                        return false;
                    }

                    if (metadata.testMethods.contains(target)) {
                        return false;
                    }

                    for (var e : graph.incomingEdgesOf(target)) {
                        if (!metadata.testMethods.contains(graph.getEdgeSource(e))) {
                            return false;
                        }
                    }

                    return true;
                })
                .filter(target -> !metadata.testMethods.contains(target))
                .collect(Collectors.toSet());

        targetsToRemove.forEach(graph::removeVertex);
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
