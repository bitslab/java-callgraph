package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class JFlexIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JFlexIT.class);

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
        Path jflexJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2.jar");
        Path jflexDependencyJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2-jar-with-dependencies.jar");
        Path jflexFullJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-full-1.8.2.jar");
        Path jflexTestJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","jflex-1.8.2-tests.jar");

        LOGGER.info("Starting JFlex Git Verification");
        assertTrue(Files.exists(jflexJar));
        assertTrue(Files.exists(jflexDependencyJar));
        assertTrue(Files.exists(jflexFullJar));
        assertTrue(Files.exists(jflexTestJar));

        // Build Stage
        Path jflexGraph = Paths.get(System.getProperty("user.dir"),"jflex_graph");
        LOGGER.info("Starting JFlex Build Verification");
        assertTrue(Files.exists(jflexGraph));


        // Test Stage @TODO - Resolve issues w/ property files

    }
}
