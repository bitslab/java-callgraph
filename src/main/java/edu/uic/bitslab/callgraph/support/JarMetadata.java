package edu.uic.bitslab.callgraph.support;

import org.apache.bcel.generic.Type;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URLClassLoader;
import java.util.*;

public class JarMetadata implements Serializable {

    private static transient final Logger LOGGER = LoggerFactory.getLogger(JarMetadata.class);
    /**
     * Keeps track of the line numbers at which a method call occurs. This is combined with Jacoco
     * line number coverage.
     *
     * <p>(MethodSignature -> Set(File:LineNumber))
     */
    public final Map<String, Set<String>> impliedMethodCalls = new HashMap<>();
    /**
     * Methods that are found in the jar for a program's tests
     */
    public final Set<String> testMethods = new HashSet<>();
    private transient final URLClassLoader cl;
    private transient final Reflections reflections;
    private transient final ClassHierarchyInspector inspector = new ClassHierarchyInspector();
    /**
     * Methods that are generated from the type erasure process
     */
    private final Set<String> bridgeMethods = new HashSet<>();
    /**
     * Methods that are created during {@link gr.gousiosg.javacg.stat.MethodVisitor#expand(Class,
     * String, Type[], Type, String)}
     */
    private final Set<String> concreteMethods = new HashSet<>();
    /**
     * Methods result in a call to {@link gr.gousiosg.javacg.stat.MethodVisitor#expand(Class, String,
     * Type[], Type, String)}
     */
    private final Set<String> virtualMethods = new HashSet<>();

    public final Set<String> callSites = new HashSet<>();

    /**
     * Wrapper class used for reflection on class hierarchies
     *
     * @param cl          the {@link ClassLoader} containing all provided jars
     * @param reflections the {@link Reflections} using the {@link cl} classloader
     */
    public JarMetadata(URLClassLoader cl, Reflections reflections) {
        this.cl = cl;
        this.reflections = reflections;
    }

    public Optional<Class<?>> getClass(String qualifiedName) {
        qualifiedName = qualifiedName.replace("/", ".");
        try {
            return Optional.of(Class.forName(qualifiedName, false, cl));
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.error("Unable to load class: " + qualifiedName);
            return Optional.empty();
        }
    }

    public Reflections getReflections() {
        return this.reflections;
    }

    public ClassHierarchyInspector getInspector() {
        return inspector;
    }

    public void addBridgeMethod(String methodSignature) {
        bridgeMethods.add(methodSignature);
    }

    public void addConcreteMethod(String methodSignature) {
        concreteMethods.add(methodSignature);
    }

    public void addCallsite(String callSite) {
        callSites.add(callSite);
    }

    public void addVirtualMethod(String methodSignature) {
        virtualMethods.add(methodSignature);
    }

    public boolean containsConcreteMethod(String methodSignature) {
        return concreteMethods.contains(methodSignature);
    }

    public boolean containsVirtualMethod(String methodSignature) {
        return virtualMethods.contains(methodSignature);
    }

    public Set<String> getBridgeMethods() {
        return new HashSet<>(bridgeMethods);
    }

    public Set<String> getConcreteMethods() {
        return new HashSet<>(concreteMethods);
    }

    public Set<String> getVirtualMethods() {
        return new HashSet<>(virtualMethods);
    }
}
