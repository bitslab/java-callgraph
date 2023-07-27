package edu.uic.bitslab.callgraph.support;

public class HelperText {
    static private final StringBuilder virtualIncludeConcrete = new StringBuilder();

    public static void printAll() {
        System.out.print(getVirtualIncludeConcrete());
    }

    public static void addVirtualIncludeConcrete(String s) {
        addVirtualIncludeConcrete(s, true);
    }

    public static void addVirtualIncludeConcrete(String s, boolean addNewLine) {
        virtualIncludeConcrete.append(s);
        if (addNewLine) virtualIncludeConcrete.append(System.lineSeparator());
    }

    public static String getVirtualIncludeConcrete() {
        return
            "=== PACKAGE YAML ===" + System.lineSeparator() +
            System.lineSeparator() +
            "virtualIncludeConcrete:" + System.lineSeparator() +
            virtualIncludeConcrete + System.lineSeparator() +
            System.lineSeparator();
    }
}
