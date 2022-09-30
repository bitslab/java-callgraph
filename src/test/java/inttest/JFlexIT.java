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
    private final Path size2nbits = Paths.get(System.getProperty("user.dir"), "output", "StateSetQuickcheck#size2nbits-reachability.dot");
    private final Path containsIsSubset = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addIsUnion = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addCommutes = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addEmpty = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addSelf = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addIdemPotent = Paths.get(System.getProperty("user.dir"), "output");
    private final Path intersect = Paths.get(System.getProperty("user.dir"), "output");
    private final Path intersectUnchanged = Paths.get(System.getProperty("user.dir"), "output");
    private final Path intersectCommutes = Paths.get(System.getProperty("user.dir"), "output");
    private final Path intersectEmpty = Paths.get(System.getProperty("user.dir"), "output");
    private final Path intersectSelf = Paths.get(System.getProperty("user.dir"), "output");
    private final Path containsItsElements = Paths.get(System.getProperty("user.dir"), "output");
    private final Path removeRemoves = Paths.get(System.getProperty("user.dir"), "output");
    private final Path removeAdd = Paths.get(System.getProperty("user.dir"), "output");
    private final Path clearMakesEmpty = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addStateAdds = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addStateDoesNotRemove = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addStateAdd = Paths.get(System.getProperty("user.dir"), "output");
    private final Path complementNoOriginalElements = Paths.get(System.getProperty("user.dir"), "output");
    private final Path complementElements = Paths.get(System.getProperty("user.dir"), "output");
    private final Path complementUnion = Paths.get(System.getProperty("user.dir"), "output");
    private final Path containsElements = Paths.get(System.getProperty("user.dir"), "output");
    private final Path containsNoElements = Paths.get(System.getProperty("user.dir"), "output");
    private final Path copy = Paths.get(System.getProperty("user.dir"), "output");
    private final Path copyInto = Paths.get(System.getProperty("user.dir"), "output");
    private final Path hashCode = Paths.get(System.getProperty("user.dir"), "output");
    private final Path getAndRemoveRemoves = Paths.get(System.getProperty("user.dir"), "output");
    private final Path getAndRemoveIsElement = Paths.get(System.getProperty("user.dir"), "output");
    private final Path getAndRemoveAdd = Paths.get(System.getProperty("user.dir"), "output");
    private final Path enumerator = Paths.get(System.getProperty("user.dir"), "output");
    private final Path invariants = Paths.get(System.getProperty("user.dir"), "output");
    private final Path maxCharCode = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addSingle = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addSingleSingleton = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addSet = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addSet-reachability.dot");
    private final Path addSetParts = Paths.get(System.getProperty("user.dir"), "output");
    private final Path addSetComplement = Paths.get(System.getProperty("user.dir"), "output", "CharClassesQuickcheck#addSetComplement-reachability.dot");
    private final Path addString = Paths.get(System.getProperty("user.dir"), "output");
    private final Path normaliseSingle = Paths.get(System.getProperty("user.dir"), "output");
    private final Path computeTablesEq = Paths.get(System.getProperty("user.dir"), "output");
    private final Path getTablesEq = Paths.get(System.getProperty("user.dir"), "output");
    private final Path classCodesUnion = Paths.get(System.getProperty("user.dir"), "output");
    private final Path classCodesCode = Paths.get(System.getProperty("user.dir"), "output");
    private final Path classCodesDisjointOrdered = Paths.get(System.getProperty("user.dir"), "output");


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
        assertTrue(Files.exists(size2nbits));
        assertTrue(Files.exists(containsIsSubset));
        assertTrue(Files.exists(addIsUnion));
        assertTrue(Files.exists(addCommutes));
        assertTrue(Files.exists(addEmpty));
        assertTrue(Files.exists(addSelf));
        assertTrue(Files.exists(addIdemPotent));
        assertTrue(Files.exists(intersect));
        assertTrue(Files.exists(intersectUnchanged));
        assertTrue(Files.exists(intersectCommutes));
        assertTrue(Files.exists(intersectEmpty));
        assertTrue(Files.exists(intersectSelf));
        assertTrue(Files.exists(containsItsElements));
        assertTrue(Files.exists(removeRemoves));
        assertTrue(Files.exists(removeAdd));
        assertTrue(Files.exists(clearMakesEmpty));
        assertTrue(Files.exists(addStateAdds));
        assertTrue(Files.exists(addStateDoesNotRemove));
        assertTrue(Files.exists(addStateAdd));
        assertTrue(Files.exists(complementNoOriginalElements));
        assertTrue(Files.exists(complementElements));
        assertTrue(Files.exists(complementUnion));
        assertTrue(Files.exists(containsElements));
        assertTrue(Files.exists(containsNoElements));
        assertTrue(Files.exists(copy));
        assertTrue(Files.exists(copyInto));
        assertTrue(Files.exists(hashCode));
        assertTrue(Files.exists(getAndRemoveRemoves));
        assertTrue(Files.exists(getAndRemoveIsElement));
        assertTrue(Files.exists(getAndRemoveAdd));
        assertTrue(Files.exists(enumerator));
        assertTrue(Files.exists(invariants));
        assertTrue(Files.exists(maxCharCode));
        assertTrue(Files.exists(addSingle));
        assertTrue(Files.exists(addSingleSingleton));
        assertTrue(Files.exists(addSet));
        assertTrue(Files.exists(addSetParts));
        assertTrue(Files.exists(addSetComplement));
        assertTrue(Files.exists(addString));
        assertTrue(Files.exists(normaliseSingle));
        assertTrue(Files.exists(computeTablesEq));
        assertTrue(Files.exists(getTablesEq));
        assertTrue(Files.exists(classCodesUnion));
        assertTrue(Files.exists(classCodesCode));
        assertTrue(Files.exists(classCodesDisjointOrdered));

    }

    //
    // Create png files for comparison
    @Test
    public void testE() throws IOException, InterruptedException {
        String cmd = "./buildpng.sh";
        ProcessBuilder pb = new ProcessBuilder(cmd);
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
