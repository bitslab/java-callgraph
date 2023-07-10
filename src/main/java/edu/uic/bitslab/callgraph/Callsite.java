package edu.uic.bitslab.callgraph;


public class Callsite {
    public static boolean isCallNode(String vertex) {
        return vertex.contains(" - ");
    }

    public static boolean isVirtual(String vertex) {
        return false;
    }
}
