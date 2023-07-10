package edu.uic.bitslab.callgraph.support;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Optional;

public class ClassHierarchyInspectorTest {

    private ClassHierarchyInspector inspector;

    @Before
    public void setup() {
        inspector = new ClassHierarchyInspector();
    }

    /**
     * B inherits "a" from A
     */
    @Test
    public void itFetchesTheTopLevelMethod() {
        Method a = HierarchyHelper.A.class.getDeclaredMethods()[0];
        Optional<Method> maybeMethod =
                inspector.getTopLevelSignature(
                        HierarchyHelper.B.class, MethodSignatureUtil.namedMethodSignature(a));
        Assert.assertTrue(maybeMethod.isPresent());
        Assert.assertEquals(a, maybeMethod.get());
    }

    @Test
    public void itDetectsOverriddenMethodsPartOne() {
        // with its own named method signature
        Method overridden = HierarchyHelper.C.class.getDeclaredMethods()[0];
        Optional<Method> maybeMethod =
                inspector.getTopLevelSignature(
                        HierarchyHelper.C.class, MethodSignatureUtil.namedMethodSignature(overridden));
        Assert.assertTrue(maybeMethod.isPresent());
        Assert.assertEquals(overridden, maybeMethod.get());
    }

    @Test
    public void itDetectsOverriddenMethodsPartTwo() {
        // with the parent classes' named method signatures
        Method overridden = HierarchyHelper.C.class.getDeclaredMethods()[0];
        Method original = HierarchyHelper.A.class.getDeclaredMethods()[0];
        Optional<Method> maybeMethod =
                inspector.getTopLevelSignature(
                        HierarchyHelper.C.class, MethodSignatureUtil.namedMethodSignature(original));
        Assert.assertTrue(maybeMethod.isPresent());
        Assert.assertEquals(overridden, maybeMethod.get());
    }
}
