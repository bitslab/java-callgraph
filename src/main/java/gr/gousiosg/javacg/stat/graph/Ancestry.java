package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Ancestry {

  private static final Logger LOGGER = LoggerFactory.getLogger(Ancestry.class);

  public static Graph<ColoredNode, DefaultEdge> compute(
      Graph<String, DefaultEdge> graph, String entrypoint, int ancestryDepth) {

    if (!graph.containsVertex(entrypoint)) {
      LOGGER.error("---> " + entrypoint + "<---");
      LOGGER.error("The graph doesn't contain the vertex specified as the entry point!");
      throw new InputMismatchException("graph doesn't contain vertex " + entrypoint);
    }

    LOGGER.info("Starting ancestry at entry point: " + entrypoint);
    LOGGER.info("Traversing to depth " + ancestryDepth);

    /* Book-keeping */
    Graph<ColoredNode, DefaultEdge> ancestry = new DefaultDirectedGraph<>(DefaultEdge.class);
    Map<String, ColoredNode> nodeMap = new HashMap<>();
    Deque<String> parentsToInspect = new ArrayDeque<>();
    Set<String> seenBefore = new HashSet<>();
    Set<String> nextLevel = new HashSet<>();

    /* Add root node to ancestry graph */
    ColoredNode root = new ColoredNode(entrypoint);
    ancestry.addVertex(root);
    nodeMap.put(entrypoint, root);
    parentsToInspect.push(entrypoint);

    int currentDepth = 0;
    while (!parentsToInspect.isEmpty()) {

      if (ancestryDepth < currentDepth) {
        break;
      }

      /* Loop over all nodes that we haven't yet seen yet and are reachable at depth "currentDepth" */
      while (!parentsToInspect.isEmpty()) {

        /* Fetch next node */
        String child = parentsToInspect.pop();
        ColoredNode childNode =
            nodeMap.containsKey(child) ? nodeMap.get(child) : new ColoredNode(child);

        /* Keep track of who we've seen before */
        seenBefore.add(child);
        if (!nodeMap.containsKey(child)) {
          ancestry.addVertex(childNode);
          nodeMap.put(child, childNode);
        }

        graph
            .incomingEdgesOf(child)
            .forEach(
                incomingEdge -> {
                  String parent = graph.getEdgeSource(incomingEdge);
                  ColoredNode parentNode =
                      nodeMap.containsKey(parent) ? nodeMap.get(parent) : new ColoredNode(parent);

                  if (!nodeMap.containsKey(parent)) {
                    nodeMap.put(parent, parentNode);
                    ancestry.addVertex(parentNode);
                  }

                  ancestry.addEdge(parentNode, childNode);

                  /* Have we visited this vertex before? */
                  if (!seenBefore.contains(parent)) {
                    nextLevel.add(parent);
                    seenBefore.add(parent);
                  }
                });
      }

      currentDepth++;
      parentsToInspect.addAll(nextLevel);
      nextLevel.clear();
    }

    nodeMap.get(entrypoint).markEntryPoint();
    return ancestry;
  }
}
