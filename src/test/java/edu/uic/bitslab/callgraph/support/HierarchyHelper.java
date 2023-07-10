package edu.uic.bitslab.callgraph.support;

public class HierarchyHelper {
    public static class A {
        public Integer a(int a, String b) {
            return Math.min(a, b.length());
        }
    }

    public static class B extends A {
        public String b(int a, String b) {
            return b + Integer.toString(a);
        }
    }

    public static class C extends B {
        @Override
        public Integer a(int a, String b) {
            return Math.max(a, b.length());
        }
    }
}
