package edu.uic.bitslab.callgraph.config;

import java.util.*;

public class ManualOptions extends PropertyAnnotation {
    @FromProperty(name = "callgraph", required = true)
    public String callgraphFilename;

    @FromProperty(name = "jacoco", required = true)
    public String jacocoFilename;

    @FromProperty(name = "output", required = true)
    public String outputFilename;

    @FromProperty(name = "jarPath", required = true)
    public String jarPath;

    @FromProperty(name = "returnType")
    public String returnType;

    @FromProperty(name = "parameterTypes")
    public String parameterTypes;

    @FromProperty(name = "classMethod")
    public String classMethod;

    @FromProperty(name = "depth")
    public int depth = -1;

    @FromProperty(name = "packages")
    public Collection<String> packages;

    @FromProperty(name = "virtual.concrete")
    public Map<String,List<String>> virtualIncludeConcrete;

    public ManualOptions() {
        set();
    }

    public ManualOptions(Properties properties) {
        set(properties);
    }
}