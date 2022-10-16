package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.After;
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

public class ConvexIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvexIT.class);
    private final Path convexJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1.jar");
    private final Path convexDependencyJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1-jar-with-dependencies.jar");
    private final Path convexTestJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1-tests.jar");
    private final Path convexGraph = Paths.get(System.getProperty("user.dir"),"convex_core_graph");
    private final Path primitiveRoundTrip = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#primitiveRoundTrip.dot");
    private final Path primitiveRoundTripReachability = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#primitiveRoundTrip-reachability.dot");
    private final Path dataRoundTripFormat = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#dataRoundTrip.dot");
    private final Path dataRoundTripReachability = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#dataRoundTrip-reachability.dot");
    private final Path messageRoundTrip = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#messageRoundTrip.dot");
    private final Path messageRoundTripReachability = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#messageRoundTrip-reachability.dot");

    @Test
    public void testA(){
        String [] args = {"git", "-c", "convex"};
        JCallGraph.main(args);
    }

    @Test
    public void testB(){
        String [] args = {"build", "-j", "./artifacts/output/convex-core-0.7.1.jar",
                "-t", "./artifacts/output/convex-core-0.7.1-tests.jar", "-o", "convex_core_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testC(){
        String [] args = {"test", "-c", "convex", "-f", "convex_core_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testD(){

        // Git Stage
        LOGGER.info("Starting Convex Git Verification");
        assertTrue(Files.exists(convexJar));
        assertTrue(Files.exists(convexDependencyJar));
        assertTrue(Files.exists(convexTestJar));

        // Build Stage
        LOGGER.info("Starting Convex Build Verification");
        assertTrue(Files.exists(convexGraph));


        // Test Stage
        LOGGER.info("Starting Convex Test Verfication");
        assertTrue(Files.exists(primitiveRoundTrip));
        assertTrue(Files.exists(primitiveRoundTripReachability));
        assertTrue(Files.exists(dataRoundTripFormat));
        assertTrue(Files.exists(dataRoundTripReachability));
        assertTrue(Files.exists(messageRoundTrip));
        assertTrue(Files.exists(messageRoundTripReachability));

    }

    //
    // Create png files for comparison
    @Test
    public void testE() throws IOException, InterruptedException {
        String cmd = "./buildpng.sh";
        String project = "convex";
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
        String project = "convex";
        ProcessBuilder pb = new ProcessBuilder(cmd, project);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null) {
            if(line.contains("%")) {
                String[] values = line.split(" : ");
                Double percentDifference = Double.parseDouble(values[1].replace("%", ""));
                Assert.assertTrue(percentDifference < 0.05);
            }
            LOGGER.info(line);
        }
        process.waitFor();
    }

    @After
    public void cleanUp() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", "rm", "output/*.dot");
        Process process = pb.start();
        process.waitFor();
    }
}
