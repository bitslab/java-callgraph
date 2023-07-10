package edu.uic.bitslab.callgraph;

import edu.uic.bitslab.callgraph.config.ManualOptions;
import junit.framework.TestCase;
import java.util.Properties;

public class ManualOptionsTest extends TestCase {
    public void testFromProperties() {
        Properties p = new Properties();
        p.setProperty("callgraph", "a");
        p.setProperty("jacoco", "b");
        p.setProperty("output", "c");
        p.setProperty("jarPath", "d");
        p.setProperty("returnType", "e");
        p.setProperty("parameterTypes", "f");
        p.setProperty("classMethod", "g");
        p.setProperty("depth", "1");
        p.setProperty("packages", "['h']");
        p.setProperty("virtual.concrete", "{'y':['z']}");

        ManualOptions manualOptions = new ManualOptions(p);
        assertEquals("a", manualOptions.callgraphFilename);
        assertEquals("b", manualOptions.jacocoFilename);
        assertEquals("c", manualOptions.outputFilename);
        assertEquals("d", manualOptions.jarPath);
        assertEquals("e", manualOptions.returnType);
        assertEquals("f", manualOptions.parameterTypes);
        assertEquals("g", manualOptions.classMethod);
        assertEquals(1, manualOptions.depth);

        assertEquals(1, manualOptions.packages.size());
        assertTrue(manualOptions.packages.contains("h"));

        assertEquals(1, manualOptions.virtualIncludeConcrete.size());
        assertTrue(manualOptions.virtualIncludeConcrete.get("y").contains("z"));
    }

    public void testFromPropertiesLots() {
        Properties p = new Properties();
        p.setProperty("callgraph", "a");
        p.setProperty("jacoco", "b");
        p.setProperty("output", "c");
        p.setProperty("jarPath", "d");
        p.setProperty("returnType", "e");
        p.setProperty("parameterTypes", "f");
        p.setProperty("classMethod", "g");
        p.setProperty("depth", "1");
        p.setProperty("packages", "['h','i','j','k','l']");
        p.setProperty("virtual.concrete", "{'y':['z'],'x':['a','b','c'],'w':['d','e','f']}");

        ManualOptions manualOptions = new ManualOptions(p);
        assertEquals("a", manualOptions.callgraphFilename);
        assertEquals("b", manualOptions.jacocoFilename);
        assertEquals("c", manualOptions.outputFilename);
        assertEquals("d", manualOptions.jarPath);
        assertEquals("e", manualOptions.returnType);
        assertEquals("f", manualOptions.parameterTypes);
        assertEquals("g", manualOptions.classMethod);
        assertEquals(1, manualOptions.depth);

        assertEquals(5, manualOptions.packages.size());
        assertTrue(manualOptions.packages.contains("h"));
        assertTrue(manualOptions.packages.contains("i"));
        assertTrue(manualOptions.packages.contains("j"));
        assertTrue(manualOptions.packages.contains("k"));
        assertTrue(manualOptions.packages.contains("l"));

        assertEquals(3, manualOptions.virtualIncludeConcrete.size());

        assertEquals(1, manualOptions.virtualIncludeConcrete.get("y").size());
        assertTrue(manualOptions.virtualIncludeConcrete.get("y").contains("z"));

        assertEquals(3, manualOptions.virtualIncludeConcrete.get("x").size());
        assertTrue(manualOptions.virtualIncludeConcrete.get("x").contains("a"));
        assertTrue(manualOptions.virtualIncludeConcrete.get("x").contains("b"));
        assertTrue(manualOptions.virtualIncludeConcrete.get("x").contains("c"));

        assertEquals(3, manualOptions.virtualIncludeConcrete.get("w").size());
        assertTrue(manualOptions.virtualIncludeConcrete.get("w").contains("d"));
        assertTrue(manualOptions.virtualIncludeConcrete.get("w").contains("e"));
        assertTrue(manualOptions.virtualIncludeConcrete.get("w").contains("f"));
    }
}