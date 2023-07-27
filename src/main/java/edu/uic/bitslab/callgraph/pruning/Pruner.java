package edu.uic.bitslab.callgraph.pruning;

import java.util.stream.Stream;

public interface Pruner {
    Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets);
}
