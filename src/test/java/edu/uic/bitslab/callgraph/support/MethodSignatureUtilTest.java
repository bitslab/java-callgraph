package edu.uic.bitslab.callgraph.support;

import edu.uic.bitslab.callgraph.support.MethodSignatureUtil;
import org.apache.bcel.generic.Type;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class MethodSignatureUtilTest {

    private static final String JACOCO_CLASS_NAME = "edu/uic/cs398/Book/BookFactory";
    private static final String JACOCO_METHOD_NAME = "getBook";
    private static final String JACOCO_METHOD_DESCRIPTOR = "(I)Ledu/uic/cs398/Book/Book;";
    private static final String EXPECTED_JACOCO_CONVERSION =
            "edu.uic.cs398.Book.BookFactory.getBook(I)Ledu/uic/cs398/Book/Book;";

    /**
     * These should all be equivalent
     */
    @Test
    public void testFullyQualifiedMethodNameEquivalence() {
        // Get class and method
        Class<?> clazz = HierarchyHelper.B.class;
        Method method = clazz.getDeclaredMethods()[0];
        String classname = MethodSignatureUtil.fullyQualifiedClassName(clazz);
        String methodName = MethodSignatureUtil.methodName(method);

        // Get BCEL types
        String signature = Type.getSignature(method);
        Type[] methodArgumentTypes = Type.getArgumentTypes(signature);
        Type methodReturnType = Type.getReturnType(signature);

        // test it once
        String actual =
                MethodSignatureUtil.fullyQualifiedMethodSignature(
                        classname, methodName, methodArgumentTypes, methodReturnType);
        String expected =
                "edu.uic.bitslab.callgraph.support.HierarchyHelper.B.b(ILjava/lang/String;)Ljava/lang/String;";
        Assert.assertEquals(expected, actual);

        // test against another variant
        String another = MethodSignatureUtil.fullyQualifiedMethodSignature(clazz, method);
        Assert.assertEquals(another, actual);

        another =
                MethodSignatureUtil.fullyQualifiedMethodSignature(
                        classname, methodName, MethodSignatureUtil.methodDescriptor(method));
        Assert.assertEquals(another, actual);

        another = MethodSignatureUtil.fullyQualifiedMethodSignature(method);
        Assert.assertEquals(another, actual);
    }

    /**
     * These should all be equivalent
     */
    @Test
    public void testMethodDescriptorEquivalence() {
        // Get class and method
        Class<?> clazz = HierarchyHelper.B.class;
        Method method = clazz.getDeclaredMethods()[0];
        String methodName = MethodSignatureUtil.methodName(method);

        // Get BCEL types
        String signature = Type.getSignature(method);
        Type[] methodArgumentTypes = Type.getArgumentTypes(signature);
        Type methodReturnType = Type.getReturnType(signature);

        // regular and named descriptors
        String regularDescriptor = "(ILjava/lang/String;)Ljava/lang/String;";
        String namedDescriptor = "b" + regularDescriptor;

        // test regular descriptors
        String actual = MethodSignatureUtil.methodDescriptor(method);
        Assert.assertEquals(regularDescriptor, actual);

        actual = MethodSignatureUtil.methodDescriptor(methodArgumentTypes, methodReturnType);
        Assert.assertEquals(regularDescriptor, actual);

        // test named descriptors
        actual = MethodSignatureUtil.namedMethodSignature(method);
        Assert.assertEquals(namedDescriptor, actual);

        actual =
                MethodSignatureUtil.namedMethodSignature(methodName, methodArgumentTypes, methodReturnType);
        Assert.assertEquals(namedDescriptor, actual);
    }

    /**
     * A Class and Method is properly resolved to the fully qualified method signature, e.g.:
     * `edu.uic.bitslab.callgraph.support.HierarchyHelper.B.b(ILjava/lang/String;)Ljava/lang/String;`
     */
    @Test
    public void testClassAndMethodToFullyQualifiedMethodSignature() {
        Method m = HierarchyHelper.B.class.getDeclaredMethods()[0];
        String actual = MethodSignatureUtil.fullyQualifiedMethodSignature(HierarchyHelper.B.class, m);
        String expected =
                "edu.uic.bitslab.callgraph.support.HierarchyHelper.B.b(ILjava/lang/String;)Ljava/lang/String;";
        Assert.assertEquals(expected, actual);
    }

    /**
     * A method signature is properly resolved, e.g.: `b(ILjava/lang/String;)Ljava/lang/String;`
     */
    @Test
    public void testMethodToMethodSignature() {
        Method m = HierarchyHelper.B.class.getDeclaredMethods()[0];
        String actual = MethodSignatureUtil.namedMethodSignature(m);
        String expected = "b(ILjava/lang/String;)Ljava/lang/String;";
        Assert.assertEquals(expected, actual);
    }

    /**
     * A method name is properly resolved, e.g.: `b`
     */
    @Test
    public void testMethodToMethodName() {
        Method m = HierarchyHelper.B.class.getDeclaredMethods()[0];
        String actual = MethodSignatureUtil.methodName(m);
        String expected = "b";
        Assert.assertEquals(expected, actual);
    }

    /**
     * A method descriptor is properly resolved, e.g.: `(ILjava/lang/String;)Ljava/lang/String;`
     */
    @Test
    public void testMethodToMethodDescriptor() {
        Method m = HierarchyHelper.B.class.getDeclaredMethods()[0];
        String actual = MethodSignatureUtil.methodDescriptor(m);
        String expected = "(ILjava/lang/String;)Ljava/lang/String;";
        Assert.assertEquals(expected, actual);
    }

    /**
     * Before: edu.uic.bitslab.callgraph.support.HierarchyHelper$B After:
     * edu.uic.bitslab.callgraph.support.HierarchyHelper.B
     */
    @Test
    public void testSanitizeClassName() {
        Class<?> clazz = HierarchyHelper.B.class;
        String name = MethodSignatureUtil.fullyQualifiedClassName(clazz);
        Assert.assertFalse(name.contains("$"));
        Assert.assertFalse(name.contains("/"));
    }

    /**
     * Before: edu/uic/cs398/Book/BookFactory After: edu.uic.cs398.Book.BookFactory
     */
    @Test
    public void testSanitizeJacocoClassName() {
        String expected = "edu.uic.cs398.Book.BookFactory";
        String name = MethodSignatureUtil.sanitizeClassName(JACOCO_CLASS_NAME);
        Assert.assertFalse(name.contains("$"));
        Assert.assertFalse(name.contains("/"));
        Assert.assertEquals(expected, name);
    }

    /**
     * The data from jacoco's XML report is converted properly
     */
    @Test
    public void testClassNameAndMethodNameAndMethodDescriptor() {
        String actual =
                MethodSignatureUtil.fullyQualifiedMethodSignature(
                        JACOCO_CLASS_NAME, JACOCO_METHOD_NAME, JACOCO_METHOD_DESCRIPTOR);
        Assert.assertEquals(EXPECTED_JACOCO_CONVERSION, actual);
    }

    /**
     * The `.`-joined components of the combined string should match the expected full string
     */
    @Test
    public void testSplitAndJoin() {
        Class<?> clazz = HierarchyHelper.B.class;
        Method method = clazz.getDeclaredMethods()[0];
        String namedMethodSignature = MethodSignatureUtil.namedMethodSignature(method);
        String className = MethodSignatureUtil.fullyQualifiedClassName(clazz);
        String combined = String.join(".", className, namedMethodSignature);
        Assert.assertEquals(combined, MethodSignatureUtil.fullyQualifiedMethodSignature(clazz, method));
    }
}
