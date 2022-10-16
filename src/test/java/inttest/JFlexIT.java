package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class JFlexIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JFlexIT.class);

    private final Path jflexJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2.jar");
    private final Path jflexDependencyJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2-jar-with-dependencies.jar");
    private final Path jflexFullJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-full-1.8.2.jar");
    private final Path jflexTestJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2-tests.jar");
    private final Path jflexGraph = Paths.get(System.getProperty("user.dir"),"jflex_graph");
    private final Path removeAdd = Paths.get(System.getProperty("user.dir"), "output", "StateSetQuickcheck#removeAdd-reachability.dot");
    private final Path addStateDoesNotRemove = Paths.get(System.getProperty("user.dir"), "output", "StateSetQuickcheck#addStateDoesNotRemove-reachability.dot");
    private final Path containsElements = Paths.get(System.getProperty("user.dir"), "output", "StateSetQuickcheck#containsElements-reachability.dot");
    private final Path addSingle = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addSingle-reachability.dot");
    private final Path addSingleSingleton = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addSingleSingleton-reachability.dot");
    private final Path addSet = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addSet-reachability.dot");
    private final Path addString = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addString-reachability.dot");

    @Test
    public void testA(){
        String [] args = {"git", "-c", "jflex"};
        JCallGraph.main(args);
    }

    @Test
    public void testB(){
        String [] args = {"build", "-j", "./artifacts/output/jflex-1.8.2.jar",
                "-t", "./artifacts/output/jflex-1.8.2-tests.jar", "-o", "jflex_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testC(){
        String [] args = {"test", "-c", "jflex", "-f", "jflex_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testD(){
        // Git Stage
        LOGGER.info("Starting JFlex Git Verification");
        assertTrue(Files.exists(jflexJar));
        assertTrue(Files.exists(jflexDependencyJar));
        assertTrue(Files.exists(jflexFullJar));
        assertTrue(Files.exists(jflexTestJar));

        // Build Stage
        LOGGER.info("Starting JFlex Build Verification");
        assertTrue(Files.exists(jflexGraph));

        // Test Stage
        LOGGER.info("Starting JFlex Test Verification");
        assertTrue(Files.exists(removeAdd));
        assertTrue(Files.exists(addStateDoesNotRemove));
        assertTrue(Files.exists(containsElements));
        assertTrue(Files.exists(addSingle));
        assertTrue(Files.exists(addSingleSingleton));
        assertTrue(Files.exists(addSet));
        assertTrue(Files.exists(addString));

    }

    //
    // Create png files for comparison
    @Test
    public void testE() throws IOException, InterruptedException {
        String cmd = "./buildpng.sh";
        String project = "jflex";
        ProcessBuilder pb = new ProcessBuilder(cmd, project);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
    }


    //
    // Test difference through diffimg
    @Test
    public void testF() throws IOException, InterruptedException {
        String cmd = "./testdiff.sh";
        String project = "jflex";
        ProcessBuilder pb = new ProcessBuilder(cmd, project);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null) {
            if(line.contains("%"))
                Assert.assertTrue(line.contains("0.0%"));
            LOGGER.info(line);
        }
        process.waitFor();
    }
}
