package edu.uic.bitslab.callgraph.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.*;

public class SUTConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SUTConfig.class);

    public String name;
    public String URL;
    public String checkoutID;
    public String patchName;
    public String subProject = "";
    public String mvnOptions = "";
    public List<Property> properties;
    public String mainJar;
    public String testJar;
    public Set<String> packages = new HashSet<>();
    public Map<String,Set<String>> virtualIncludeConcrete = new HashMap<>();

    private static final String CONFIG_PATH = "artifacts/configs/{}/{}.yaml";

    public static SUTConfig fromProjectName(String name) {
        if (name == null || name.isEmpty()) return null;
        return fromYAML(new File(CONFIG_PATH.replace("{}", name)));
    }

    public static SUTConfig fromYAML(File yamlFile) {
        try (InputStream inputStream = new FileInputStream(yamlFile)) {
            Yaml yaml = new Yaml(new Constructor(SUTConfig.class));
            return yaml.load(inputStream);
        } catch (IOException e) {
            LOGGER.error("IOException: " + e.getMessage());
        }

        LOGGER.error("Could not obtain yaml file!");
        return null;
    }

    public String getProjectDir() {
            return (this.subProject == null || this.subProject.isEmpty())
                ? this.name
                : (this.name + "/" + this.subProject);
    }

    public static class Property {
        public String name;
        public String entryPoint;

        public Property() {

        }

        public Property(String name, String entryPoint) {
            this.name = name;
            this.entryPoint = entryPoint;
        }
    }
}
