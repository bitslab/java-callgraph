package edu.uic.bitslab.callgraph;

import edu.uic.bitslab.callgraph.config.SUTConfig;
import junit.framework.TestCase;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

public class SUTConfigTest extends TestCase {

    public void testFromYAML() throws URISyntaxException {
        URL yamlURL = this.getClass().getClassLoader().getResource("testFromYAML.yaml");
        assertNotNull(yamlURL);

        URI yamlURI = yamlURL.toURI();
        File yamlFile = new File(yamlURI);

        SUTConfig sutConfig = SUTConfig.fromYAML(yamlFile);
        assertNotNull(sutConfig);

        assertEquals("test-yaml", sutConfig.name);
        assertEquals("https://invalid/test/project", sutConfig.URL);
        assertEquals("deadbeef", sutConfig.checkoutID);
        assertEquals("some/named/test-yaml.patch", sutConfig.patchName);
        assertEquals("subTest", sutConfig.subProject);
        assertEquals("main-project.jar", sutConfig.mainJar);
        assertEquals("test-project.jar", sutConfig.testJar);
        assertEquals("-Dmaven.surefire.debug=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000\"", sutConfig.mvnOptions);

        assertEquals(3, sutConfig.properties.size());
        assertEquals("One", sutConfig.properties.get(0).name);
        assertEquals("invalid.test.one(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V", sutConfig.properties.get(0).entryPoint);

        assertEquals("Two", sutConfig.properties.get(1).name);
        assertEquals("invalid.test.two(S)V", sutConfig.properties.get(1).entryPoint);

        assertEquals("Three", sutConfig.properties.get(2).name);
        assertEquals("invalid.test.three(I)V", sutConfig.properties.get(2).entryPoint);

        assertEquals(3, sutConfig.packages.size());
        assertTrue(sutConfig.packages.containsAll(Set.of("invalid.test", "another.test", "yet.one.more.test")));

        assertTrue(sutConfig.virtualIncludeConcrete.containsKey("some-virtual"));
        assertEquals(1, sutConfig.virtualIncludeConcrete.get("some-virtual").size());
        assertTrue(sutConfig.virtualIncludeConcrete.get("some-virtual").contains("concrete-one"));

        assertTrue(sutConfig.virtualIncludeConcrete.containsKey("one-more-virtual"));
        assertEquals(3, sutConfig.virtualIncludeConcrete.get("one-more-virtual").size());
        assertTrue(sutConfig.virtualIncludeConcrete.get("one-more-virtual").contains("concrete-one"));
        assertTrue(sutConfig.virtualIncludeConcrete.get("one-more-virtual").contains("concrete-two"));
        assertTrue(sutConfig.virtualIncludeConcrete.get("one-more-virtual").contains("concrete-three"));
    }

    public void testFromYAMLInvalidFile() throws URISyntaxException {
        URL yamlURL = this.getClass().getClassLoader().getResource("testFromYAMLInvalidFile.yaml");
        assertNotNull(yamlURL);

        URI yamlURI = yamlURL.toURI();
        File yamlFile = new File(yamlURI);

        try {
            SUTConfig.fromYAML(yamlFile);
            fail("Should have thrown exception.");
        } catch (YAMLException yamlException) {
            assertTrue(yamlException.getMessage().startsWith("Cannot create property=name"));
        }
    }
}