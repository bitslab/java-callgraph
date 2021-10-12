package gr.gousiosg.javacg.stat.coverage;

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

public class JacocoCoverage {

  public static final String METHOD_TYPE = "METHOD";
  public static final String LINE_TYPE = "LINE";
  public static final String BRANCH_TYPE = "BRANCH";
  private static final Logger LOGGER = LoggerFactory.getLogger(JacocoCoverage.class);
  private boolean hasCoverage = false;

  private Map<String, Report.Package.Class.Method> methodCoverage = new HashMap<>();

  /**
   * Create a {@link JacocoCoverage} object
   *
   * @param maybeFilepath the JaCoCo coverage XML file to parse
   */
  public JacocoCoverage(Optional<String> maybeFilepath)
      throws IOException, ParserConfigurationException, JAXBException, SAXException {

    if (maybeFilepath.isPresent()) {
      /* Convert the jacoco.xml file into a Report object */
      Report report = JacocoCoverageParser.getReport(maybeFilepath.get());

      /* Iterate over all packages in report */
      for (Report.Package pkg : report.getPackage()) {

        /* Iterate over all classes in a package */
        for (Report.Package.Class clazz : pkg.getClazz()) {

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
        }
      }

      /* Indicate that coverage has been applied */
      hasCoverage = true;
    }
  }

  private static Map<String, ColoredNode> nodeMap(Set<ColoredNode> nodes) {
    return nodes.stream().collect(Collectors.toMap(ColoredNode::getLabel, node -> node));
  }

  public void applyCoverage(Graph<ColoredNode, DefaultEdge> graph) {
    LOGGER.info("Applying coverage!");
    Map<String, ColoredNode> nodeMap = nodeMap(graph.vertexSet());
    nodeMap
        .keySet()
        .forEach(
            node -> {
              if (methodCoverage.containsKey(node)) {
                Report.Package.Class.Method m = methodCoverage.get(node);
                nodeMap.get(node).mark(m);
              } else {
                LOGGER.warn("Couldn't find coverage for " + node);
                nodeMap.get(node).markMissing();
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
        case JacocoCoverage.BRANCH_TYPE:
          {
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
}