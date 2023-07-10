package edu.uic.bitslab.callgraph.pruning;

import edu.uic.bitslab.callgraph.Callsite;
import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;
import edu.uic.bitslab.callgraph.graph.StaticCallgraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class PruneMethods {
    private String entryPointName;
    private StaticCallgraph callgraph;
    private JacocoCoverage coverage;
    private Pruner[] pruners;

    public PruneMethods(String entryPointName, StaticCallgraph callgraph, JacocoCoverage coverage) {
        this.entryPointName = entryPointName;
        this.callgraph = callgraph;
        this.coverage = coverage;
        pruners = new Pruner[]{
//                new KeepMethodsWithCoverage(coverage),
//                new KeepMethodsInsidePackage("com.indeed")
                new OnlyKeep12ConcreteTargetsWithCoverage(coverage)
        };
    }

    public final void prune() {
        LinkedList<String> nodes = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        nodes.addLast(entryPointName);

        while (!nodes.isEmpty()) {
            String node = nodes.removeFirst();
            seen.add(node);

            Set<DefaultEdge> edges = this.callgraph.graph.outgoingEdgesOf(node);

            if (Callsite.isCallNode(node)) {
                Set<String> concreteTargets = edges.stream().map(this.callgraph.graph::getEdgeTarget).collect(Collectors.toSet());
                Set<String> pruned = this.pruneConcreteTargets(node, concreteTargets);
                Set<String> kept   = concreteTargets.stream().filter(not(pruned::contains)).collect(Collectors.toSet());

                pruned.stream().forEach(e -> callgraph.graph.removeEdge(node, e));
            }

            edges.stream().map(callgraph.graph::getEdgeTarget).filter(not(seen::contains)).forEach(nodes::addLast);
        }

    }

    protected Set<String> pruneConcreteTargets(String virtualCall, Set<String> concreteTargets) {
        Stream<String> targets = concreteTargets.stream();

        for (Pruner p : pruners)
            targets = p.pruneConcreteTargets(virtualCall, targets);

        return targets.collect(Collectors.toSet());
    }
}
