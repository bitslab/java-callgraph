package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.graph.StaticCallgraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class PruneMethods {
    private String entryPointName;
    private StaticCallgraph callgraph;
    private JacocoCoverage coverage;

    public PruneMethods(String entryPointName, StaticCallgraph callgraph, JacocoCoverage coverage) {
        this.entryPointName = entryPointName;
        this.callgraph = callgraph;
        this.coverage = coverage;
    }

    private final boolean isVirtualCallNode(String vertex) {
        return vertex.contains(" - ");
    }

    public final void prune() {
        LinkedList<String> nodes = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        nodes.addLast(entryPointName);

        while (!nodes.isEmpty()) {
            String node = nodes.removeFirst();
            seen.add(node);

            Set<DefaultEdge> edges = this.callgraph.graph.outgoingEdgesOf(node);

            if (this.isVirtualCallNode(node)) {
                Set<String> concreteTargets = edges.stream().map(this.callgraph.graph::getEdgeTarget).collect(Collectors.toSet());
                Set<String> pruned = this.pruneConcreteTargets(node, concreteTargets);
                Set<String> kept   = concreteTargets.stream().filter(not(pruned::contains)).collect(Collectors.toSet());

                pruned.stream().forEach(e -> callgraph.graph.removeEdge(node, e));
            }

            edges.stream().map(callgraph.graph::getEdgeTarget).filter(not(seen::contains)).forEach(nodes::addLast);
        }

    }

    protected Set<String> pruneConcreteTargets(String virtualCall, Set<String> concreteTargets) {
        return concreteTargets.stream()
                .filter(concreteMethod -> !coverage.hasNonzeroCoverage(concreteMethod))
                .filter(m -> !m.contains("com.indeed"))
                .collect(Collectors.toSet());
    }
}
