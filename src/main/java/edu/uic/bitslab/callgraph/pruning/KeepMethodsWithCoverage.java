package edu.uic.bitslab.callgraph.pruning;

import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;

import java.util.stream.Stream;

public class KeepMethodsWithCoverage implements Pruner {
    private JacocoCoverage coverage;

    public KeepMethodsWithCoverage(JacocoCoverage coverage) {
        this.coverage = coverage;
    }

    @Override
    public Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets) {
        return concreteTargets.filter(concreteMethod -> !coverage.hasNonzeroCoverage(concreteMethod));
    }
}
