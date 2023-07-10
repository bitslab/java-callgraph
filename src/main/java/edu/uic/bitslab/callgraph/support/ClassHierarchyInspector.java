package edu.uic.bitslab.callgraph.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassHierarchyInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassHierarchyInspector.class);

    /**
     * { {@link Class} -> { {@link MethodSignatureUtil#namedMethodSignature(Method)} -> {@link Method}
     * } }
     */
    Map<Class<?>, Map<String, Method>> classDeclaredMethods = new HashMap<>();

    /**
     * Memoize the declared methods of a class hierarchy into {@link classDeclaredMethods}
     *
     * @param clazz the {@link Class} to expand the hierarchy of
     */
    private void loadHierarchy(Class<?> clazz) {
        try {
            if (classDeclaredMethods.containsKey(clazz)) {
                // We're done! This hierarchy has been explored already...
                return;
            } else {
                // ... We need to explore this class hierarchy!
                classDeclaredMethods.putIfAbsent(clazz, new HashMap<>());
            }

            // Memoize the declared methods of this class.
            // May contain overridden methods
            Arrays.stream(clazz.getDeclaredMethods())
                    .forEach(
                            method -> {
                                String namedSignature = MethodSignatureUtil.namedMethodSignature(method);
                                classDeclaredMethods.get(clazz).put(namedSignature, method);
                            });

            // Traverse the hierarchy and load it
            Optional<Class<?>> maybeParent = Optional.ofNullable(clazz.getSuperclass());
            maybeParent.ifPresent(this::loadHierarchy);
        } catch (Exception | NoClassDefFoundError e) {
            LOGGER.error("Trouble loading hierarchy for " + clazz.getName());
        }
    }

    /**
     * @param clazz                a {@link Class}
     * @param namedMethodSignature a method signature resembling {@link
     *                             MethodSignatureUtil#namedMethodSignature(Method)}
     * @return a {@link Optional<Method>}
     */
    public Optional<Method> getTopLevelSignature(Class<?> clazz, String namedMethodSignature) {
        try {
            // Ensure the hierarchy is loaded
            loadHierarchy(clazz);
            while (clazz != null) {
                if (classDeclaredMethods.containsKey(clazz)
                        && classDeclaredMethods.get(clazz).containsKey(namedMethodSignature)) {
                    // Retrieve the method associated with `clazz` and `namedMethodSignature`
                    return Optional.of(classDeclaredMethods.get(clazz).get(namedMethodSignature));
                }
                // Traverse the class hierarchy of `clazz`
                clazz = clazz.getSuperclass();
            }
        } catch (Exception | NoClassDefFoundError e) {
            LOGGER.error(
                    "Unable to find method " + namedMethodSignature + " in class " + clazz.getName());
        }
        return Optional.empty();
    }
}
