package edu.uic.bitslab.callgraph.pruning;

import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;
import edu.uic.bitslab.callgraph.support.HelperText;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlyKeep12ConcreteTargetsWithCoverage implements Pruner {
    private final JacocoCoverage coverage;
    private final Map<String, Set<String>> virtualIncludeConcrete;

    public OnlyKeep12ConcreteTargetsWithCoverage(JacocoCoverage coverage, Map<String, Set<String>> virtualIncludeConcrete) {
        this.virtualIncludeConcrete = virtualIncludeConcrete;
        this.coverage = coverage;
    }

    @Override
    public Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets) {
        // Is this virtualCall defined in the config
        Set<String> includeConcrete = this.virtualIncludeConcrete.get(virtualCall);
        if (includeConcrete != null) {
            return concreteTargets.filter(Predicate.not(includeConcrete::contains));
        }

        // not configured... so how many concrete targets were covered?
        Set<String> orig = concreteTargets.collect(Collectors.toSet());
        Set<String> tgts = orig.stream().filter(coverage::hasNonzeroCoverage).collect(Collectors.toSet());

        int coverageSize = tgts.size();

        if (coverageSize == 0) {
            // No coverage, prune all
            return orig.stream();
        }

        if (coverageSize >= 3) {
            // log helper details for yaml
            HelperText.addVirtualIncludeConcrete(
                // virtual call
                "\t\"" + virtualCall + "\":" + System.lineSeparator() +

                // add covered first
                orig.stream().filter(tgts::contains).map( s -> "\t- \"" + s + "\"" )
                    .collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator() +

                // add not covered next
                orig.stream().filter(Predicate.not(tgts::contains)).map( s -> "\t- \"" + s + "\"" )
                    .collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator()
            );
        }

        return Stream.empty();

        // return not covered to be pruned
        // return orig.stream().filter(Predicate.not(tgts::contains));
    }
}
