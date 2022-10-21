package gr.gousiosg.javacg.stat.coverage;

import gr.gousiosg.javacg.stat.support.JarMetadata;
import gr.gousiosg.javacg.stat.support.MethodSignatureUtil;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static gr.gousiosg.javacg.stat.graph.Utilities.nodeMap;

public class JacocoCoverage {

    public static final String METHOD_TYPE = "METHOD";
    public static final String LINE_TYPE = "LINE";
    public static final String BRANCH_TYPE = "BRANCH";
    private static final Logger LOGGER = LoggerFactory.getLogger(JacocoCoverage.class);
    @SuppressWarnings("UnusedAssignment")
    private boolean hasCoverage = false;

    private final Map<String, Report.Package.Class.Method> methodCoverage = new HashMap<>();
    private final Set<String> coveredLines = new HashSet<>();

    /**
     * Create a {@link JacocoCoverage} object
     *
     * @param path the JaCoCo coverage XML file to parse
     */
    public JacocoCoverage(String path)
            throws IOException, ParserConfigurationException, JAXBException, SAXException {

        /* Convert the jacoco.xml file into a Report object */
        Report report = JacocoCoverageParser.getReport(path);

        /* Iterate over all packages in report */
        for (Report.Package pkg : report.getPackage()) {

            /* Iterate over all classes in a package */
            pkg.getClazz()
                    .forEach(
                            clazz -> {
                                /* Find all methods in a class */
                                List<Report.Package.Class.Method> methods =
                                        clazz.getContent().stream()
                                                .filter(s -> s instanceof JAXBElement)
                                                .map(s -> (JAXBElement<?>) s)
                                                .filter(je -> je.getValue() instanceof Report.Package.Class.Method)
                                                .map(je -> (Report.Package.Class.Method) je.getValue())
                                                .collect(Collectors.toList());

                                /* Store "covered" methods in methodCoverage */
                                methods.forEach(
                                        method -> {
                                            String qualifiedName =
                                                    MethodSignatureUtil.fullyQualifiedMethodSignature(
                                                            clazz.getName(), method.getName(), method.getDesc());
                                            methodCoverage.putIfAbsent(qualifiedName, method);
                                        });
                            });

            /* Iterate over all source files in a package */
            pkg.getSourcefile()
                    .forEach(
                            rawSrcFile ->
                                    rawSrcFile.getContent().stream()
                                            .filter(s -> s instanceof JAXBElement)
                                            .map(s -> (JAXBElement<?>) s)
                                            .filter(je -> je.getValue() instanceof Report.Package.Sourcefile.Line)
                                            .map(je -> (Report.Package.Sourcefile.Line) je.getValue())
                                            .filter(
                                                    line ->
                                                            Byte.toUnsignedInt(line.cb) > 0 || Byte.toUnsignedInt(line.ci) > 0)
                                            .forEach(
                                                    line ->
                                                            coveredLines.add(
                                                                    String.format(
                                                                            "%s:%d",
                                                                            rawSrcFile.getName(), Short.toUnsignedInt(line.nr)))));
        }

        /* Indicate that coverage has been applied */
        hasCoverage = true;
    }

    public void applyCoverage(Graph<ColoredNode, DefaultEdge> graph, JarMetadata metadata) {
        LOGGER.info("Applying coverage!");
        Map<String, ColoredNode> nodeMap = nodeMap(graph.vertexSet());
        nodeMap
                .keySet()
                .forEach(
                        method -> {
                            if (metadata.testMethods.contains(method)) {
                                nodeMap.get(method).setExcluded(true);
                                return;
                            }

                            if (methodCoverage.containsKey(method)) {
                                Report.Package.Class.Method m = methodCoverage.get(method);
                                nodeMap.get(method).mark(m);
                            } else {
                                // didn't find it in methodCoverage? let's see if there is implied coverage...

                                Set<String> impliedCalls = metadata.impliedMethodCalls.get(method);
                                if (impliedCalls == null) {
                                    LOGGER.warn("Couldn't find coverage for " + method);
                                    nodeMap.get(method).markMissing();
                                    return;
                                }

                                boolean impliedCoverage =
                                        impliedCalls.stream()
                                                .anyMatch(fileAndLine -> coveredLines.contains(fileAndLine));

                                if (impliedCoverage) {
                                    nodeMap.get(method).markImpliedCoverage();
                                } else {
                                    LOGGER.warn("Couldn't find coverage for " + method);
                                    nodeMap.get(method).markMissing();
                                }
                            }
                        });
    }

    public boolean hasCoverage() {
        return hasCoverage;
    }

    public boolean containsMethod(String methodSignature) {
        return methodCoverage.containsKey(methodSignature);
    }

    public boolean hasNonzeroCoverage(String methodSignature) {
        if (!containsMethod(methodSignature)) {
            return false;
        }

        Report.Package.Class.Method method = methodCoverage.get(methodSignature);

        for (Report.Package.Class.Method.Counter counter : method.getCounter()) {
            switch (counter.getType()) {
                case JacocoCoverage.METHOD_TYPE:
                case JacocoCoverage.LINE_TYPE:
                case JacocoCoverage.BRANCH_TYPE: {
                    if (hasNonzeroCoverage(counter)) {
                        return true;
                    }
                }
                default:
            }
        }

        return false;
    }

    private boolean hasNonzeroCoverage(Report.Package.Class.Method.Counter counter) {
        return counter.getCovered() > 0;
    }

    public Map<String, Report.Package.Class.Method> getMethodCoverage() {
        //methodCoverage.get(a).
        return methodCoverage;
    }
}
