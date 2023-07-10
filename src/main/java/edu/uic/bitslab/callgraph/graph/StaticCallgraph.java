package edu.uic.bitslab.callgraph.graph;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.ClassVisitor;
import edu.uic.bitslab.callgraph.support.BuildArguments;
import edu.uic.bitslab.callgraph.support.JarMetadata;
import org.apache.bcel.classfile.ClassParser;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static edu.uic.bitslab.callgraph.graph.Utilities.*;

public class StaticCallgraph implements Serializable {

    private static transient final Logger LOGGER = LoggerFactory.getLogger(StaticCallgraph.class);

    public JarMetadata metadata;
    public SerializableDefaultDirectedGraph<String, DefaultEdge> graph;
    public String JarEntry;

    private StaticCallgraph(Graph<String, DefaultEdge> graph, JarMetadata jarMetadata) {
        this.graph = (SerializableDefaultDirectedGraph<String, DefaultEdge>) graph;
        this.metadata = jarMetadata;
    }

    /**
     * Builds a static callgraph from the provided jars
     *
     * @param buildArguments the arguments to build the graph with
     * @return a {@link Graph} representing the static callgraph of the combined jars
     * @throws InputMismatchException
     */
    public static StaticCallgraph build(BuildArguments buildArguments) throws InputMismatchException {
        LOGGER.info("Beginning callgraph analysis...");
        var jars = buildArguments.getJars();
        var maybeTestJar = buildArguments.getMaybeTestJar();

        // 1. SETTING UP FOR GRAPH INSPECTION
        /* Load JAR URLs */
        List<URL> urls = new ArrayList<>();
        try {
            for (Pair<String, File> pair : jars) {
                URL url = new URL("jar:file:" + pair.first + "!/");
                urls.add(url);
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Error loading URLs: " + e.getMessage());
            throw new InputMismatchException("Couldn't load provided JARs");
        }

        if (urls.isEmpty()) {
            LOGGER.error("No URLs to scan!");
            throw new InputMismatchException("There are no URLs to scan!");
        }

        /* Setup infrastructure for analysis */
        URLClassLoader cl =
                URLClassLoader.newInstance(urls.toArray(new URL[0]), Utilities.class.getClassLoader());
        Reflections reflections = new Reflections(cl, new SubTypesScanner(false));
        JarMetadata jarMetadata = new JarMetadata(cl, reflections);

        /* Store method calls (caller -> receiver) */
        Map<String, Set<String>> calls = new HashMap<>();

        // 2. GRAPH INSPECTION
        /* iterate over all provided jars */
        for (Pair<String, File> pair : jars) {
            String jarPath = pair.first;
            File file = pair.second;

            try (JarFile jarFile = new JarFile(file)) {
                boolean isTestJar = (maybeTestJar.isPresent() && jarPath.equals(maybeTestJar.get().first));

                LOGGER.info("Analyzing: " + jarFile.getName());
                Stream<JarEntry> entries = enumerationAsStream(jarFile.entries());

                Function<ClassParser, ClassVisitor> getClassVisitor =
                        (ClassParser cp) -> {
                            try {
                                return new ClassVisitor(cp.parse(), jarMetadata, isTestJar);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        };

                /* Analyze each jar entry to find callgraph */
                inspectJarEntries(entries, jarPath, getClassVisitor, calls);

            } catch (IOException e) {
                LOGGER.error("Error when analyzing JAR \"" + jarPath + "\": + e.getMessage()");
                e.printStackTrace();
            }
        }

        /* Convert calls into a graph */
        Graph<String, DefaultEdge> graph = buildGraph(calls);
        return new StaticCallgraph(graph, jarMetadata);
    }

    /**
     * Takes all (source, destination) method call pairs and stitches them together to create a graph
     *
     * @param methodCalls the method calls
     * @return a {@link Graph}
     * @throws InputMismatchException
     */
    private static Graph<String, DefaultEdge> buildGraph(Map<String, Set<String>> methodCalls)
            throws InputMismatchException {
        if (methodCalls.keySet().isEmpty()) {
            throw new InputMismatchException("There is no call graph to look at!");
        }

        Graph<String, DefaultEdge> graph = new SerializableDefaultDirectedGraph<>(DefaultEdge.class);
        methodCalls
                .keySet()
                .forEach(
                        source -> {
                            /* create source vertex */
                            putIfAbsent(graph, source);

                            methodCalls
                                    .get(source)
                                    .forEach(
                                            destination -> {
                                                /* create destination vertex */
                                                putIfAbsent(graph, destination);

                                                /* connect every (source, destination) pair with an edge */
                                                graph.addEdge(source, destination);
                                            });
                        });

        return graph;
    }

    /**
     * Finds all (source, destination) method call pairs within a jar
     *
     * @param entries         the entries of the jar
     * @param jarPath         the path to the jar
     * @param getClassVisitor a {@link ClassVisitor}
     * @param calls           the data structure containing all (source, destination) method call pairs
     */
    private static void inspectJarEntries(
            Stream<JarEntry> entries,
            String jarPath,
            Function<ClassParser, ClassVisitor> getClassVisitor,
            Map<String, Set<String>> calls) {

        /* Analyze each jar entry to find callgraph */
        entries
                .flatMap(
                        e -> {
                            /* Only inspect directories and `*.class` files */
                            if (e.isDirectory() || !e.getName().endsWith(".class")) return Stream.of();

                            /* Ignore specified JARs */
                            if (shouldIgnoreEntry(e.getName().replace("/", "."))) {
                                return Stream.of();
                            } else {
                                LOGGER.info("Inspecting " + e.getName());
                            }

                            ClassParser cp = new ClassParser(jarPath, e.getName());
                            return getClassVisitor.apply(cp).start().methodCalls().stream();
                        })
                .forEach(
                        p -> {
                            /* Create edges between nodes */
                            calls.putIfAbsent((p.first), new HashSet<>());
                            calls.get(p.first).add(p.second);
                        });
    }
}
