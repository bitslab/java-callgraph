package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ConvexIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvexIT.class);

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
        Path convexJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1.jar");
        Path convexDependencyJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1-jar-with-dependencies.jar");
        Path convexTestJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","convex-core-0.7.1-tests.jar");
        LOGGER.info("Starting Convex Git Verification");
        assertTrue(Files.exists(convexJar));
        assertTrue(Files.exists(convexDependencyJar));
        assertTrue(Files.exists(convexTestJar));

        // Build Stage
        Path convexGraph = Paths.get(System.getProperty("user.dir"),"convex_core_graph");
        LOGGER.info("Starting Convex Build Verification");
        assertTrue(Files.exists(convexGraph));


        // Test Stage
        Path genTestFormat = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#primitiveRoundTrip.dot");
        Path genTestFormatReachability = Paths.get(System.getProperty("user.dir"),"output","GenTestFormat#primitiveRoundTrip-reachability.dot");
        assertTrue(Files.exists(genTestFormat));
        assertTrue(Files.exists(genTestFormatReachability));

    }
}
