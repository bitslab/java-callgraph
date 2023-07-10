package edu.uic.bitslab.callgraph.pruning;

import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class KeepMethodsInsidePackage implements Pruner {
    private String pak;

    public KeepMethodsInsidePackage(String pak) {
        this.pak = pak;
    }

    @Override
    public Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets) {
        return concreteTargets.filter(not(concreteTarget -> concreteTarget.startsWith(pak)));
    }
}
