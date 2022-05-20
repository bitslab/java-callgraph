/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import edu.uic.bitslab.callgraph.GetBest;
import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.CoverageStatistics;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.graph.*;
import gr.gousiosg.javacg.stat.support.BuildArguments;
import gr.gousiosg.javacg.stat.support.GitArguments;
import gr.gousiosg.javacg.stat.support.RepoTool;
import gr.gousiosg.javacg.stat.support.TestArguments;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives into a single call
 * graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Will Cygan <wcygan3232@gmail.com>
 * @author Alekh Meka <alekhmeka@gmail.com>
 */
public class JCallGraph {

  public static final String OUTPUT_DIRECTORY = "./output/";
  private static final Logger LOGGER = LoggerFactory.getLogger(JCallGraph.class);
  private static final String REACHABILITY = "reachability";
  private static final String COVERAGE = "coverage";
  private static final String ANCESTRY = "ancestry";
  private static final String DELIMITER = "-";
  private static final String DOT_SUFFIX = ".dot";
  private static final String CSV_SUFFIX = ".csv";
  private static final String SER_SUFFIX = ".ser";

  public static void main(String[] args) {
    try {
      LOGGER.info("Starting java-cg!");
      switch(args[0]){
        case "manual-test": {
          manualMain(args);
          return;
        }
        case "git":{
          GitArguments arguments = new GitArguments(args);
          RepoTool rt = maybeObtainTool(arguments);
          rt.cloneRepo();
          rt.applyPatch();
          rt.buildJars();
          break;
        }
        case "build": {
          // Build and serialize a staticcallgraph object with jar files provided
          BuildArguments arguments = new BuildArguments(args);
          StaticCallgraph callgraph = StaticCallgraph.build(arguments);
          maybeSerializeStaticCallGraph(callgraph, arguments);
          break;
        }
        case "test": {
          TestArguments arguments = new TestArguments(args);
          // 1. Run Tests and obtain coverage
          RepoTool rt = maybeObtainTool(arguments);
          List<Pair<String, String>> coverageFilesAndEntryPoints = rt.obtainCoverageFilesAndEntryPoints();
          for(Pair<String, String> s : coverageFilesAndEntryPoints) {
            // 2. For each coverage file we start with a fresh deserialized callgraph
            StaticCallgraph callgraph = deserializeStaticCallGraph(arguments);
            LOGGER.info("----------PROPERTY------------");
            String propertyName = s.first.substring(s.first.lastIndexOf("/") + 1, s.first.length() - 4);
            LOGGER.info(propertyName);
            rt.testProperty(propertyName);
            JacocoCoverage jacocoCoverage = new JacocoCoverage(s.first);
            // 3. Prune the graph with coverage
            Pruning.pruneOriginalGraph(callgraph, jacocoCoverage);
            // 4. Operate on the graph and write it to output
            maybeWriteGraph(callgraph.graph, JCallGraph.OUTPUT_DIRECTORY + propertyName);
            Graph<ColoredNode, DefaultEdge> prunedReachability = maybeInspectReachability(callgraph, arguments.maybeDepth(), jacocoCoverage, s.second, JCallGraph.OUTPUT_DIRECTORY + propertyName);
            maybeInspectAncestry(callgraph, arguments, jacocoCoverage, Optional.of(s.second), Optional.of(propertyName));
            rt.cleanTarget();

            // write the best paths and annotated dot file
            GetBest getBest = new GetBest(prunedReachability, propertyName);
            getBest.run();
          }
          break;
        }

        default:
          LOGGER.error("Invalid argument provided!");
          System.exit(1);
      }
    } catch (InputMismatchException e) {
      LOGGER.error("Unable to load callgraph: " + e.getMessage());
      System.exit(1);
    } catch(JGitInternalException e){
      LOGGER.error("Cloned directory already exists!");
      System.exit(1);
    } catch(FileNotFoundException e){
      LOGGER.error("Error obtaining valid yaml folder path: " + e.getMessage());
      System.exit(1);
    } catch (ParserConfigurationException | SAXException | JAXBException | IOException e) {
      LOGGER.error("Error fetching Jacoco coverage: " + e.getMessage());
      System.exit(1);
    } catch(ClassNotFoundException e){
      LOGGER.error("Error creating class through deserialization");
      System.exit(1);
    } catch (GitAPIException e) {
      LOGGER.error("Error cloning repository");
      System.exit(1);
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted during applying patches/building jars");
      System.exit(1);
    }

    LOGGER.info("java-cg is finished! Enjoy!");

  }

  public static void manualMain(String[] args) {

    // First argument:   the serialized file
    StaticCallgraph callgraph = null;
    try {
      File f = new File(args[1]);
      LOGGER.info("Deserializing file " + f.getAbsolutePath());
      callgraph = deserializeStaticCallGraph(new File(args[1]));
    } catch (IOException e) {
      LOGGER.error("Could not deserialize static call graph", e);
    } catch (ClassNotFoundException e) {
      LOGGER.error("This shouldn't happen, go fix your CLASSPATH", e);
    }

    // Second argument: the jacoco.xml
    JacocoCoverage jacocoCoverage = null;
    try {
      File f = new File(args[2]);
      LOGGER.info("Reading JaCoCo coverage file " + f.getAbsolutePath());
      jacocoCoverage = new JacocoCoverage(f.getAbsolutePath());
    } catch (IOException | ParserConfigurationException | JAXBException | SAXException e) {
      LOGGER.error("Could not read JaCoCo coverage file", e);
    }

    // Third argument:  the entry point
    String entryPoint = args[3];

    // Fourth argument:  the output file
    String output = args[4];

    if (callgraph == null || jacocoCoverage == null) {
      // Something went wrong, bail
      return;
    }

    // Fifth argument, optional, is the depth
    Optional<Integer> depth = Optional.empty();
    if (args.length > 5)
      depth = Optional.of(Integer.parseInt(args[5]));

    // This method changes the callgraph object
    Pruning.pruneOriginalGraph(callgraph, jacocoCoverage);

    maybeInspectReachability(callgraph, depth, jacocoCoverage, args[3], args[4]);

    // maybeWriteGraph(callgraph.graph, args[4]);
  }

  private static void maybeWriteGraph(Graph<String, DefaultEdge> graph, String output) {
    Utilities.writeGraph(graph, Utilities.defaultExporter(), JCallGraph.asDot(output));
  }

  private static Graph<ColoredNode, DefaultEdge> maybeInspectReachability(
      StaticCallgraph callgraph, Optional<Integer> depth, JacocoCoverage jacocoCoverage, String entryPoint, String outputFile) {

    /* Fetch reachability */
    Graph<ColoredNode, DefaultEdge> reachability =
        Reachability.compute(
            callgraph.graph, entryPoint, depth);

    /* Apply coverage */
    jacocoCoverage.applyCoverage(reachability, callgraph.metadata);

    Pruning.pruneReachabilityGraph(reachability, callgraph.metadata, jacocoCoverage);

    /* Should we write the graph to a file? */
    String outputName = getCompleteOutputName(depth, outputFile);

    /* Store reachability in file? */
    Utilities.writeGraph(
            reachability, Utilities.coloredExporter(), JCallGraph.asDot(outputName));

    try {
      writeSerializeReachabilityGraph(reachability, asSer(outputName));
    } catch (IOException e) {
      LOGGER.error("Error writing serialized reachability graph.");
      LOGGER.error(e.getMessage());
    }


    /* Analyze reachability coverage? */
    if (jacocoCoverage.hasCoverage()) {
      CoverageStatistics.analyze( reachability, Optional.of(asCsv(outputName + DELIMITER + COVERAGE)));
    }

    return reachability;
  }

  private static String getCompleteOutputName(Optional<Integer> depth, String outputFile) {
    /* Should we write the graph to a file? */
    String outputName = outputFile + DELIMITER + REACHABILITY;

    /* Attach depth to name if present */
    if (depth.isPresent()) {
      outputName = outputName + DELIMITER + depth.get();
    }

    return outputName;
  }

  private static void maybeInspectAncestry(
      StaticCallgraph callgraph, TestArguments arguments, JacocoCoverage jacocoCoverage, Optional<String> entryPoint, Optional<String>outputName) {
    if (arguments.maybeAncestry().isEmpty() || entryPoint.isEmpty()) {
      return;
    }

    Graph<ColoredNode, DefaultEdge> ancestry =
        Ancestry.compute(
            callgraph.graph, entryPoint.get(), arguments.maybeAncestry().get());
    jacocoCoverage.applyCoverage(ancestry, callgraph.metadata);

    /* Should we store the ancestry in a file? */
    if (outputName.isPresent()) {
      String subgraphOutputName =
          outputName.get()
              + DELIMITER
              + ANCESTRY
              + DELIMITER
              + arguments.maybeAncestry().get();
      Utilities.writeGraph(
          ancestry, Utilities.coloredExporter(), JCallGraph.OUTPUT_DIRECTORY + asDot(subgraphOutputName));
    }
  }

  private static String asDot(String name) {
    return name.endsWith(DOT_SUFFIX) ? name : (name + DOT_SUFFIX);
  }

  private static String asSer(String name) {
    return name.endsWith(SER_SUFFIX) ? name : (name + SER_SUFFIX);
  }

  private static String asCsv(String name) {
    return name.endsWith(CSV_SUFFIX) ? name : (name + CSV_SUFFIX);
  }

  //
  // serializeStaticCallGraph creates a file that contains the bytecode data of the StaticCallgraph object
  // Throws: IOException when the file cannot be written to disk
  private static void maybeSerializeStaticCallGraph(StaticCallgraph callgraph, BuildArguments arguments) throws IOException{
    if(arguments.maybeOutput().isPresent()) {
      File filename = new File(arguments.maybeOutput().get());
      FileOutputStream file = new FileOutputStream(filename);
      ObjectOutputStream out = new ObjectOutputStream(file);
      out.writeObject(callgraph);
      out.close();
      file.close();
    }
  }

  private static void writeSerializeReachabilityGraph(Graph<ColoredNode, DefaultEdge> reachability, String pathname) throws IOException {
    File filename = new File(pathname);
    FileOutputStream file = new FileOutputStream(filename);
    ObjectOutputStream out = new ObjectOutputStream(file);
    out.writeObject(reachability);
    out.close();
    file.close();
  }

  //
  // deserializeStaticCallGraph reads bytecode and creates a StaticCallgraph object to be returned
  // Throws: IOException when file cannot be read
  // Throws: ClassNotFoundException when object cannot be read properly
  private static StaticCallgraph deserializeStaticCallGraph(TestArguments arguments) throws IOException, ClassNotFoundException{
    return deserializeStaticCallGraph(new File(arguments.maybeBytecodeFile().get()));
  }

  private static StaticCallgraph deserializeStaticCallGraph(File f) throws IOException, ClassNotFoundException{
    try (ObjectInput ois = new ObjectInputStream(new FileInputStream(f))) {
      return (StaticCallgraph) ois.readObject();
    }
  }


  private static RepoTool maybeObtainTool(GitArguments arguments) throws FileNotFoundException{
    Optional<RepoTool> rt = RepoTool.obtainTool(arguments.maybeGetConfig().get());
    if(rt.isPresent())
      return rt.get();
    throw new FileNotFoundException("folderName path is incorrect! Please provide a valid folder");
  }

  private static RepoTool maybeObtainTool(TestArguments arguments) throws FileNotFoundException{
    return new RepoTool(arguments.maybeGetConfig().get());
  }
}
