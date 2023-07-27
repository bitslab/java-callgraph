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
import edu.uic.bitslab.callgraph.config.ManualOptions;
import edu.uic.bitslab.callgraph.graph.*;
import edu.uic.bitslab.callgraph.support.*;
import gr.gousiosg.javacg.dyn.Pair;
import edu.uic.bitslab.callgraph.coverage.CoverageStatistics;
import edu.uic.bitslab.callgraph.coverage.JacocoCoverage;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.jgrapht.Graph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

import static java.util.Map.entry;

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
          callgraph.JarEntry=arguments.getJars().get(0).first;
          maybeSerializeStaticCallGraph(callgraph, arguments);
          break;
        }
        case "buildyaml":{
          DumperOptions options = new DumperOptions();
          options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
          options.setPrettyFlow(true);
          Yaml yaml = new Yaml(options);

          JarInputStream jarFileStream = new JarInputStream(new FileInputStream(args[1]));
          JarFile jarFile = new JarFile(args[1]);

          ArrayList<JarEntry> listOfAllClasses = getAllClassesFromJar(jarFileStream);
          ArrayList<Pair<String, String>> nameEntryList = new ArrayList<>();
          for (JarEntry entry : listOfAllClasses)
            nameEntryList.addAll(fetchAllMethodSignaturesForyaml(jarFile,entry));
          ArrayList<Map<String,String>> entryResult = new ArrayList<>();

          for(Pair<String, String> entry : nameEntryList)
            entryResult.add(Map.ofEntries(entry("name",entry.first),entry("entryPoint",entry.second)));

          Map<String, ArrayList<Map<String,String>>> dataMap = new HashMap<>();
          dataMap.put("properties",entryResult);
          final FileWriter writer = new FileWriter(args[2]+".yaml");
          yaml.dump(dataMap, writer);
          break;
        }
        case "test": {
          TestArguments arguments = new TestArguments(args);
          String entryPoint = null;
          // 1. Run Tests and obtain coverage
          RepoTool rt = maybeObtainTool(arguments);
          StaticCallgraph callgraph = deserializeStaticCallGraph(arguments);
          List<Pair<String, ?>> coverageFilesAndEntryPointsShorthand = rt.obtainCoverageFilesAndEntryPoints();
          List<Pair<String, String>> coverageFilesAndEntryPoints=new ArrayList<>();
          for(Pair<String, ?> s : coverageFilesAndEntryPointsShorthand) {
            Pair<String,String> result=new Pair<>(s.first,null);
            if(s.second instanceof String){
              entryPoint= (String) s.second;
            }
            else if(s.second instanceof ArrayList){
              try {
                Optional<String> returnType = Optional.empty();
                if(((ArrayList) s.second).size() > 1)
                  returnType = Optional.of(((ArrayList) s.second).get(1).toString());

                // Seventh argument, optional, parameter types of expected method
                Optional<String> paramterTypes = Optional.empty();
                if(((ArrayList) s.second).size() > 2)
                  paramterTypes = Optional.of(((ArrayList) s.second).get(2).toString());

                entryPoint = generateEntryPoint(callgraph.JarEntry, (String) ((ArrayList) s.second).get(0), returnType, paramterTypes);
              } catch(IOException e){
                LOGGER.error("Could not generate method signature", e);
              }
            }
            LOGGER.info("Entry point inferred for name \n"+s.first.substring(s.first.lastIndexOf("/")+1)+" is\n "+entryPoint);
            result.second=entryPoint;
            coverageFilesAndEntryPoints.add(result);
          }

          for(Pair<String, String> s : coverageFilesAndEntryPoints) {
            // 2. For each coverage file we start with a fresh deserialized callgraph
            callgraph = deserializeStaticCallGraph(arguments);
            LOGGER.info("----------PROPERTY------------");
            String propertyName = s.first.substring(s.first.lastIndexOf("/") + 1, s.first.length() - 4);
            LOGGER.info(propertyName);
            rt.testProperty(propertyName);
            JacocoCoverage jacocoCoverage = new JacocoCoverage(s.first);
            // 3. Prune the graph with coverage
            Pruning.pruneOriginalGraph(entryPoint, callgraph, jacocoCoverage, rt.getSUTConfig());
            // 4. Operate on the graph and write it to output
            maybeWriteGraph(callgraph.graph, JCallGraph.OUTPUT_DIRECTORY + propertyName);
            Graph<ColoredNode, DefaultEdge> prunedReachability = maybeInspectReachability(callgraph, arguments.maybeDepth(), jacocoCoverage, s.second, JCallGraph.OUTPUT_DIRECTORY + propertyName);
            maybeInspectAncestry(callgraph, arguments, jacocoCoverage, Optional.of(s.second), Optional.of(propertyName));

            try {
              // write the best paths and annotated dot file
              GetBest getBest = new GetBest(prunedReachability, propertyName);
              getBest.run();
            } catch (NullPointerException e) {
              // ok ... getbest blew up ... log it an continue
              LOGGER.error("Get Best Null Pointer Exception: " + Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString));
            }

            try {
              rt.moveOutput();
            } catch (Exception exception) {
              LOGGER.error("Error moving output to artifact: " + exception.getMessage());
              System.exit(1);
            }
            rt.cleanTarget();
          }

          // printAll
          HelperText.printAll();

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

  //Main function to convert class.method arg and generate its respective method signature
  public static String generateEntryPoint(String jarPath, String shortName, Optional<String> returnType, Optional<String> parameterTypes) throws IOException {
    JarFile jarFile = new JarFile(jarPath);
    JarInputStream jarFileStream = new JarInputStream(new FileInputStream(jarPath));

    String methodName = shortName.substring(shortName.lastIndexOf('.') + 1);
    String className = shortName.substring(0, shortName.lastIndexOf('.'));
    ArrayList<JarEntry> listOfFilteredClasses = getAllClassesFromJar(jarFileStream);
    className = className.replaceAll("\\.", "/") + ".class";

    listOfFilteredClasses = getFilteredClassesFromJar(listOfFilteredClasses, className);

    if(listOfFilteredClasses.size() > 1) {
      LOGGER.error("Multiple class instances found as listed below:- ");
      for(JarEntry entry : listOfFilteredClasses)
        LOGGER.error(entry.getName());
      System.exit(1);
    }
    if(listOfFilteredClasses.size()==0){
      LOGGER.error("no class instances found ");
      System.exit(1);
    }

    return fetchMethodSignatures(jarFile, listOfFilteredClasses.get(0), methodName, returnType, parameterTypes);

  }

  //Fetch JarEntry of all classes in a Jan using JarInputStream
  public static ArrayList<JarEntry> getAllClassesFromJar(JarInputStream JarInputStream) throws IOException {
    JarEntry jar;
    ArrayList<JarEntry> listOfAllClasses = new ArrayList<>();
    while(true) {
      jar = JarInputStream.getNextJarEntry();
      if(jar == null)
        break;
      if((jar.getName().endsWith(".class")))
        listOfAllClasses.add(jar);
    }
    return listOfAllClasses;
  }

  //Fetch filtered classes from a list of JarEntry
  public static ArrayList<JarEntry> getFilteredClassesFromJar(ArrayList<JarEntry> listOfAllClasses, String className) {
    ArrayList<JarEntry> listOfFilteredClasses= new ArrayList<>();
    for (JarEntry entry : listOfAllClasses)
      if (entry.getName().endsWith(className))
        listOfFilteredClasses.add(entry);
    return listOfFilteredClasses;
  }
public static ArrayList<Pair<String, String>> fetchAllMethodSignaturesForyaml (JarFile JarFile,JarEntry jar) throws IOException {
  ClassParser cp = new ClassParser(JarFile.getInputStream(jar), jar.getName());
  JavaClass jc = cp.parse();

  Method[] methods = jc.getMethods();
  String className =jc.getClassName().substring(jc.getClassName().lastIndexOf(".")+1);
  ArrayList<Pair<String, String>> signatureResults = new ArrayList<>();
  for(Method tempMethod : methods)
    if(Arrays.stream(tempMethod.getAnnotationEntries())
            .map(e->e.getAnnotationType())
            .anyMatch(e->e.equals("Lcom/pholser/junit/quickcheck/Property;"))){  // .anyMatch(e->e.equals("Lorg/junit/Test;"))){
      String methodDescriptor=tempMethod.getName() + tempMethod.getSignature();
      signatureResults.add(new Pair<>(className+"#"+tempMethod.getName(),jc.getClassName() + "." + methodDescriptor));
    }
  return signatureResults;
}
  //Fetch the method signature of a method from a JarEntry
  public static String fetchMethodSignatures(JarFile JarFile, JarEntry jar, String methodName, Optional<String> returnType, Optional<String> paramterTypes) throws IOException {
    ClassParser cp = new ClassParser(JarFile.getInputStream(jar), jar.getName());
    JavaClass jc = cp.parse();

    Method[] methods = jc.getMethods();
    ArrayList<Method> signatureResults = new ArrayList<>();

    for(Method tempMethod : methods)
      if(tempMethod.getName().equals(methodName))
        signatureResults.add(tempMethod);

    if(returnType.isPresent()) {
      ArrayList<Method> tempsignatureResults = new ArrayList<>();
      for(Method tempMethod : signatureResults)
        if(tempMethod.getReturnType().toString().contains(returnType.get()))
          tempsignatureResults.add(tempMethod);
      signatureResults=new ArrayList<>(tempsignatureResults);

      if(paramterTypes.isPresent()) {
        String[] paramlist = paramterTypes.get().split(",");
        for(Method tempMethod : signatureResults)
          if(Arrays.equals(paramlist, Arrays.stream(tempMethod.getArgumentTypes())
                  .map(Type::toString)
                  .map(e -> e.substring(e.lastIndexOf(".") + 1))
                  .toArray()))
            return jc.getClassName() + "." + tempMethod.getName() + tempMethod.getSignature();
      }
      validateMethodList(signatureResults);
      return jc.getClassName() + "." + signatureResults.get(0).getName() + signatureResults.get(0).getSignature();
    } else {
      validateMethodList(signatureResults);
      return jc.getClassName() + "." + signatureResults.get(0).getName() + signatureResults.get(0).getSignature();
    }
  }

  // Check the size of list and submit Logger info for the methods
  public static void validateMethodList(ArrayList<Method> methodList) {
    if(methodList.size() > 1) {
      LOGGER.error("Multiple overloaded methods for the given method name");
      for(Method method : methodList) {
        LOGGER.info("Name:- " + method.getName() + " Return Type:- " + method.getReturnType().toString());
        LOGGER.info("Parameter Types:- ");
        for(Type t : method.getArgumentTypes()) {
          LOGGER.info(t.toString());
        }
        method.getArgumentTypes();
      }
      System.exit(1);
    } else if(methodList.size() == 0) {
      LOGGER.info("Incorrect arguments supplied");
      System.exit(1);
    }
  }

  public static void manualMain(String[] args) {
    ManualOptions manualOptions = new ManualOptions();

    // fill in args if we have them
    if (args.length > 1) manualOptions.callgraphFilename = args[1];
    if (args.length > 2) manualOptions.jacocoFilename = args[2];
    if (args.length > 3) manualOptions.outputFilename = args[3];
    if (args.length > 4) manualOptions.jarPath = args[4];
    if (args.length > 5) manualOptions.classMethod = args[5];
    if (args.length > 6) manualOptions.returnType = args[6];
    if (args.length > 7) manualOptions.parameterTypes = args[7];


    // First argument:   the serialized file
    StaticCallgraph callgraph = null;
    try {
      File f = new File(manualOptions.callgraphFilename);
      LOGGER.info("Deserializing file " + f.getAbsolutePath());
      callgraph = deserializeStaticCallGraph(new File(manualOptions.callgraphFilename));
    } catch (IOException e) {
      LOGGER.error("Could not deserialize static call graph", e);
    } catch (ClassNotFoundException e) {
      LOGGER.error("This shouldn't happen, go fix your CLASSPATH", e);
    }

    // Second argument: the jacoco.xml
    JacocoCoverage jacocoCoverage = null;
    try {
      File f = new File(manualOptions.jacocoFilename);
      LOGGER.info("Reading JaCoCo coverage file " + f.getAbsolutePath());
      jacocoCoverage = new JacocoCoverage(f.getAbsolutePath());
    } catch (IOException | ParserConfigurationException | JAXBException | SAXException e) {
      LOGGER.error("Could not read JaCoCo coverage file", e);
    }

    if (callgraph == null || jacocoCoverage == null) {
      // Something went wrong, bail
      return;
    }

    // third argument:  the output file
    String output = manualOptions.outputFilename;

    // forth argument:  Jar path to infer entry point signature
    String jarPath = manualOptions.jarPath;
    try {
      new JarFile(jarPath);
    } catch(IOException e){
      LOGGER.error("Could not read inference Jar file", e);
    }

    // Sixth argument, optional, return type of expected method
    Optional<String> returnType = manualOptions.returnType == null ? Optional.empty() : Optional.of(manualOptions.returnType);

    // Seventh argument, optional, parameter types of expected method
    Optional<String> parameterTypes = manualOptions.parameterTypes == null ? Optional.empty() : Optional.of(manualOptions.parameterTypes);

    // Fifth argument, class.method input where class can be written as nested classes to generate exact method signature
    String entryPoint = null;
    try {
      entryPoint = generateEntryPoint(jarPath, manualOptions.classMethod, returnType, parameterTypes);
//            System.out.println(entryPoint);
    } catch(IOException e){
      LOGGER.error("Could not generate method signature", e);
    }

    // Seventh argument, optional, is the depth
    Optional<Integer> depth = manualOptions.depth == -1 ? Optional.empty() : Optional.of(manualOptions.depth);

    // This method changes the callgraph object
    Pruning.pruneOriginalGraph(entryPoint, callgraph, jacocoCoverage, manualOptions);

    maybeInspectReachability(callgraph, depth, jacocoCoverage, entryPoint, output);

    // maybeWriteGraph(callgraph.graph, manualOptions.outputFilename);


    writeXML(callgraph.graph, entryPoint, new File("dfs.xml"));
  }

  static private long depth = 0;

  private static void writeXML(Graph<String, DefaultEdge> graph, String entryPoint, File output) {
    DepthFirstIterator<String, DefaultEdge> iter = new DepthFirstIterator<>(graph, entryPoint);
    Set<String> present = new HashSet<>();

    try (PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(output)))) {
      iter.addTraversalListener(new TraversalListenerAdapter<>(){
        @Override
        public void vertexTraversed(VertexTraversalEvent<String> e) {
//          if (present.contains(e.getSource()))
//            return;
          String spaces = Stream.iterate("", i -> " ").limit(depth).reduce("", String::concat);
          ps.println(String.format("%s<node method=\"%s\">", spaces, e.getVertex().replace("<","&lt;").replace(">", "&gt;")));

          depth += 1;
        }

        @Override
        public void vertexFinished(VertexTraversalEvent<String> e) {
          depth -= 1;

          String spaces = Stream.iterate("", i -> " ").limit(depth).reduce("", String::concat);
          ps.println(String.format("%s</node>", spaces));

        }
      });

      while (iter.hasNext()) {
        iter.next();
      }
    } catch (IOException e) {
      throw new Error(e);
    }
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

//    Pruning.pruneReachabilityGraph(reachability, callgraph.metadata, jacocoCoverage);

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
    return new RepoTool(arguments.maybeGetConfig().get());
  }

  private static RepoTool maybeObtainTool(TestArguments arguments) throws FileNotFoundException{
    return new RepoTool(arguments.maybeGetConfig().get());
  }
}
