package edu.uic.bitslab.callgraph.support;

import org.apache.bcel.generic.Type;

import java.lang.reflect.Method;

public class MethodSignatureUtil {

    public static String fullyQualifiedMethodSignature(
            String className, String methodName, String methodDescriptor) {
        return String.join(".", sanitizeClassName(className), methodName) + methodDescriptor;
    }

    public static String fullyQualifiedMethodSignature(Class<?> clazz, Method method) {
        return fullyQualifiedMethodSignature(
                fullyQualifiedClassName(clazz), methodName(method), methodDescriptor(method));
    }

    public static String fullyQualifiedMethodSignature(Method method) {
        return fullyQualifiedMethodSignature(
                fullyQualifiedClassName(method.getDeclaringClass()),
                methodName(method),
                methodDescriptor(method));
    }

    public static String fullyQualifiedMethodSignature(
            String className, String methodName, Type[] methodArgumentTypes, Type methodReturnType) {
        return fullyQualifiedMethodSignature(
                sanitizeClassName(className),
                methodName,
                methodDescriptor(methodArgumentTypes, methodReturnType));
    }

    public static String fullyQualifiedClassName(Class<?> clazz) {
        return sanitizeClassName(clazz.getName());
    }

    public static String namedMethodSignature(
            String methodName, Type[] methodArgumentTypes, Type methodReturnType) {
        return methodName + methodDescriptor(methodArgumentTypes, methodReturnType);
    }

    public static String namedMethodSignature(Method m) {
        return methodName(m) + methodDescriptor(m);
    }

    public static String methodName(Method m) {
        return m.getName();
    }

    public static String methodDescriptor(Method m) {
        return Type.getSignature(m);
    }

    public static String methodDescriptor(Type[] methodArgumentTypes, Type methodReturnType) {
        return Type.getMethodSignature(methodReturnType, methodArgumentTypes);
    }

    protected static String sanitizeClassName(String className) {
        return className.replace("/", ".").replace("$", ".");
    }
}
