package edu.uic.bitslab.callgraph.prunning;

import java.util.Set;
import java.util.stream.Stream;

public interface Pruner {
    public Stream<String> pruneConcreteTargets(String virtualCall, Stream<String> concreteTargets);
}
