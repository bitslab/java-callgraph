package edu.uic.bitslab.callgraph.support;

import java.util.Set;

public class IgnoredConstants {

    /**
     * Do not expand method calls with these names
     */
    public static final Set<String> IGNORED_METHOD_NAMES = Set.of("<init>", "<clinit>");

    /**
     * Do not look into jar entries with these prefixes
     */
    public static final Set<String> IGNORED_CALLING_PACKAGES =
            Set.of(
                    "java.",
                    "javax.",
                    "javassist.",
                    "org.slf4j",
                    "org.apache",
                    "guru.",
                    "com.kitfox",
                    "org.reflections",
                    "org.webjars",
                    "net.arnx",
                    "com.google",
                    "io.cucumber",
                    "org.hamcrest",
                    "com.eclipsesource",
                    "org.checkerframework",
                    "org.antlr",
                    "org.jheaps",
                    "org.jgrapht",
                    "com.linkedin",
                    "it.unimi",
                    "freemarker.",
                    "com.martiansoftware");
}
