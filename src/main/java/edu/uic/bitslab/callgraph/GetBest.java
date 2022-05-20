package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.stat.JCallGraph;
import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.graph.Utilities;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class GetBest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetBest.class);
    private final Graph<ColoredNode, DefaultEdge> reachability;
    private final String propertyName;
    private final Map<ColoredNode, Double> score = new HashMap<>();

    /* Colors - Copied from gr.gousiosg.javacg.stat.coverage.ColoredNode */
    private static final String IMPLIED_COVERAGE_COLOR = "skyblue";
    private static final String LIGHT_GREEN = "greenyellow";
    private static final String MEDIUM_GREEN = "green1";
    private static final String MEDIUM_DARK_GREEN = "green3";
    private static final String DARK_GREEN = "green4";
    private static final String FIREBRICK = "lightpink";
    private static final String ENTRYPOINT_COLOR = "lightgoldenrod";
    private static final String NO_COLOR = "ghostwhite";
    private static final String TEST_NODE_COLOR = "plum";

    // color by type
    private static final String UNCOVERED_COLOR = FIREBRICK;



    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String objectFileName = args[0];
        String propertyName;

        if (args.length == 2) {
            propertyName = args[1];
        } else {
            String filename = Path.of(args[0]).getFileName().toString();
            propertyName = filename.substring(0, filename.lastIndexOf('.'));
        }

        LOGGER.info("----------GRAPH------------");
        LOGGER.info(objectFileName);

        GetBest o = new GetBest(objectFileName, propertyName);
        o.run();
    }

    public GetBest(Graph<ColoredNode, DefaultEdge> reachability, String propertyName) {
        this.reachability = reachability;
        this.propertyName = propertyName;
    }

    public GetBest(String objectFileName, String propertyName) throws IOException, ClassNotFoundException {
        try (ObjectInput ois = new ObjectInputStream(new FileInputStream(objectFileName))) {
            reachability = returnGraph(ois.readObject());
        }
        this.propertyName = propertyName;
    }

    @SuppressWarnings("unchecked")
    private Graph<ColoredNode, DefaultEdge> returnGraph(Object o) {
        if (o instanceof Graph) {
            return (Graph<ColoredNode, DefaultEdge>) o;
        }

        LOGGER.error("Expected instanceof Graph, but received " + o.getClass().getName() + " instead.");
        return null;
    }

    public void run() {
        // we don't have a graph to reason about!
        if (reachability == null) {
            return;
        }

        ColoredNode entryPoint = getEntryPoint();

        if (entryPoint == null) {
            LOGGER.error("Unable to find entry point");
            return;
        }

        DepthFirstIterator<ColoredNode, DefaultEdge> iter = new DepthFirstIterator<>(reachability, entryPoint);
        iter.addTraversalListener(new GetBestTraversalListener(reachability, score));

        while (iter.hasNext()) {
            iter.next();
        }


        /* annotated graph - Write to .dot file in output directory */
        String path = JCallGraph.OUTPUT_DIRECTORY + propertyName + "-annotated.dot";
        try {
            Writer writer = new FileWriter(path);
            DOTExporter<ColoredNode, DefaultEdge> exporter = Utilities.coloredExporter();
            exporter.setVertexAttributeProvider(
                    (v) -> {
                        Map<String, Attribute> map = new LinkedHashMap<>();
                        map.put("label", DefaultAttribute.createAttribute(score.get(v).toString() + " - " + dotFormat(v.toString())));
                        map.put("style", DefaultAttribute.createAttribute("filled"));
                        map.put("fillcolor", DefaultAttribute.createAttribute(v.getColor()));
                        return map;
                    });
            exporter.exportGraph(reachability, writer);
            LOGGER.info("Graph written to " + path + "!");
        } catch (IOException e) {
            LOGGER.error("Unable to write callgraph to " + path);
        }


        // get all of the paths
        HashSet<ColoredNode> sourceSet = new HashSet<>(Collections.singleton(entryPoint));
        HashSet<ColoredNode> targetSet = reachability.vertexSet().stream().filter(vertex -> reachability.outDegreeOf(vertex) == 0).collect(Collectors.toCollection(HashSet::new));
        AllDirectedPaths<ColoredNode, DefaultEdge> allDirectedPaths = new AllDirectedPaths<>(reachability);
        List<GraphPath<ColoredNode, DefaultEdge>> allPaths = allDirectedPaths.getAllPaths(sourceSet, targetSet, true, null);

        List<PathWeight> pathWeights = new ArrayList<>();

        // add the weights to each path
        allPaths.forEach( executionPath -> {
          double pathSum = executionPath.getEdgeList().stream().map(reachability::getEdgeTarget).mapToDouble(score::get).filter( (d) -> !Double.isNaN(d)).sum();
          pathWeights.add(new PathWeight(executionPath, pathSum));
        });

        // output sorted paths
        Comparator<PathWeight> comparator = Comparator.comparingDouble(p -> p.weight);
        pathWeights.sort(comparator.reversed());

        String outputPaths = JCallGraph.OUTPUT_DIRECTORY + propertyName + "-paths.csv";
        try {
            Writer writer = new FileWriter(outputPaths);
            for(PathWeight pathWeight : pathWeights) {
                writer.write(
                    pathWeight.weight +
                        "," +
                        pathWeight.path
                                .getVertexList()
                                .stream()
                                .map(p -> '"' + p.toString() + '"')
                                .collect(Collectors.joining(",")) +
                        System.lineSeparator());
            }

            writer.close();
        } catch (IOException e) {
            LOGGER.error("Unable to write paths to " + outputPaths);
        }
    }

    private ColoredNode getEntryPoint() {
        Set<ColoredNode> vertexSet = reachability.vertexSet();

        for (ColoredNode v : vertexSet) {
            if (reachability.inDegreeOf(v) == 0) {
                return v;
            }
        }

        return null;
    }

    private static String dotFormat(String vertex) {
        return "\"" + vertex + "\"";
    }

    static class PathWeight {
        public final GraphPath<ColoredNode,DefaultEdge> path;
        public final Double weight;

        PathWeight(GraphPath<ColoredNode,DefaultEdge> path, Double weight) {
            this.path = path;
            this.weight = weight;
        }
    }

    static class GetBestTraversalListener extends TraversalListenerAdapter<ColoredNode, DefaultEdge> {
        Graph<ColoredNode, DefaultEdge> graph;
        Map<ColoredNode, Double> score;

        GetBestTraversalListener(Graph<ColoredNode, DefaultEdge> graph, Map<ColoredNode, Double> score) {
            super();

            this.graph = graph;
            this.score = score;
        }

        @Override
        public void connectedComponentFinished(ConnectedComponentTraversalEvent connectedComponentTraversalEvent) {

        }

        @Override
        public void connectedComponentStarted(ConnectedComponentTraversalEvent connectedComponentTraversalEvent) {

        }

        @Override
        public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> edgeTraversalEvent) {

        }

        @Override
        public void vertexTraversed(VertexTraversalEvent<ColoredNode> vertexTraversalEvent) {

        }

        @Override
        public void vertexFinished(VertexTraversalEvent<ColoredNode> vertexTraversalEvent) {
            ColoredNode parentVertex = vertexTraversalEvent.getVertex();
            score.put(parentVertex, Score(parentVertex));
        }

        private byte vertexColorToInt(String color) {
            switch (color) {
                case UNCOVERED_COLOR:
                    return 8;

                case LIGHT_GREEN:
                    return 4;

                case MEDIUM_GREEN:
                    return 3;

                case MEDIUM_DARK_GREEN:
                    return 2;

                case DARK_GREEN:
                    return 1;

                default:
                    return 0;
            }
        }

        private double Score(ColoredNode vertex) {
            final double weightParentScore = 1;
            final double weightUncoveredChildren = .5;

            // parent score (note: high score is better
            long parentScore = vertexColorToInt(vertex.getColor());

            // get uncovered children
            long countUncoveredChildren = graph.outgoingEdgesOf(vertex)
                    .stream()
                    .map( e -> graph.getEdgeTarget(e).getColor() )
                    .filter( color -> color.equals(UNCOVERED_COLOR))
                    .count();

            return (weightParentScore * parentScore) +
                    (parentScore * (weightUncoveredChildren * countUncoveredChildren));
        }
    }
}

