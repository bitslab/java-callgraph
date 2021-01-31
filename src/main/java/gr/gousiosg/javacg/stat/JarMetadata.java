package gr.gousiosg.javacg.stat;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;
import java.util.Optional;

public class JarMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarMetadata.class);

    private final URLClassLoader cl;
    private final Reflections reflections;
    private final ClassHierarchyInspector inspector = new ClassHierarchyInspector();

    public JarMetadata(URLClassLoader cl, Reflections reflections) {
        this.cl = cl;
        this.reflections = reflections;
    }

    public Optional<Class<?>> getClass(String qualifiedName) {
        qualifiedName = qualifiedName.replace("/", ".");
        try {
            return Optional.ofNullable(cl.loadClass(qualifiedName));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Reflections getReflections() {
        return this.reflections;
    }

    public ClassHierarchyInspector getInspector() {
        return inspector;
    }

}
