package edu.uic.bitslab.callgraph.pruning;

import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlyKeep12ConcreteTargetsWithCoverage implements Pruner {

    private JacocoCoverage coverage;

    public OnlyKeep12ConcreteTargetsWithCoverage(JacocoCoverage coverage) {
        this.coverage = coverage;
    }

    @Override
    public Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets) {
        // How many concrete targets were covered?
        Set<String> orig = concreteTargets.collect(Collectors.toSet());
        Set<String> tgts = orig.stream().filter(concreteMethod -> coverage.hasNonzeroCoverage(concreteMethod)).collect(Collectors.toSet());

        switch (tgts.size()) {
            case 0:
                // No coverage, prune all
                return orig.stream();
            case 1:
            case 2:
//            default:
                // Only 1 or 2 concrete targets covered, prune others
                return orig.stream().filter(Predicate.not(tgts::contains));
            default:
                // 3 or more concrete targets covered, prune none
                return Stream.empty();
        }
    }
}
