/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import edu.uic.bitslab.callgraph.support.ClassHierarchyInspector;
import edu.uic.bitslab.callgraph.support.JarMetadata;
import edu.uic.bitslab.callgraph.support.MethodSignatureUtil;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static edu.uic.bitslab.callgraph.support.IgnoredConstants.IGNORED_METHOD_NAMES;

/**
 * The simplest of method visitors, prints any invoked method signature for all method invocations.
 *
 * <p>Class copied with modifications from CJKM: http://www.spinellis.gr/sw/ckjm/
 */
public class MethodVisitor extends EmptyVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodVisitor.class);
    private static final Boolean EXPAND = true;
    private static final Boolean DONT_EXPAND = false;
    private final JarMetadata jarMetadata;
    JavaClass visitedClass;
    private boolean isTestMethod;
    private MethodGen mg;
    private ConstantPoolGen cp;
    private String format;
    private Set<Pair<String, String>> methodCalls = new HashSet<>();
    private Map<Class<?>, Map<String, Set<String>>> expansions = new HashMap<>();
    private int currentLineNumber = -1;

    public MethodVisitor(MethodGen m, JavaClass jc, JarMetadata jarMetadata, boolean isTestMethod) {
        this.jarMetadata = jarMetadata;
        this.isTestMethod = isTestMethod;
        visitedClass = jc;
        mg = m;
        cp = mg.getConstantPool();
        format = "%s";
    }

    private String argumentList(Type[] arguments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(arguments[i].toString());
        }
        return sb.toString();
    }

    private LinkedList<InstructionHandle> findLeaders(InstructionList il) {
        // https://www.geeksforgeeks.org/basic-blocks-in-compiler-design/
        LinkedList<InstructionHandle> is = new LinkedList<>();
        Set<InstructionHandle> leaders = new HashSet<>();

        leaders.add(il.getStart());

        for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
            is.addLast(ih);
            Instruction i = ih.getInstruction();

            if (i instanceof IfInstruction) {
                IfInstruction ifi = (IfInstruction) i;
                leaders.add(ifi.getTarget());
                leaders.add(ih.getNext());
            } else if (i instanceof GOTO) {
                // TODO unconditional GOTOs
            } else if (i instanceof Select) {
                // TODO switch-case
            } else if (i instanceof ReturnInstruction || i instanceof ATHROW) {
                if (ih.getNext() != null)
                    leaders.add(ih.getNext());
            }
        }

        LinkedList<InstructionHandle> sortedLeaders = new LinkedList<>(leaders);
        Collections.sort(sortedLeaders, Comparator.comparingInt(InstructionHandle::getPosition));

        return sortedLeaders;
    }

    private List<LinkedList<InstructionHandle>> computeBasicBlocks(InstructionList il) {
        LinkedList<InstructionHandle> leaders = findLeaders(il);

        LinkedList<LinkedList<InstructionHandle>> ret = new LinkedList<>();

        LinkedList<InstructionHandle> currentBlock = new LinkedList<>();

        {
            // First instruction is always a leader, add manually
            leaders.removeFirst();
            currentBlock.addLast(il.getStart());
        }

        for (InstructionHandle ih = il.getStart().getNext(); ih != null; ih = ih.getNext()) {
            if (!leaders.isEmpty() && ih == leaders.getFirst()) {
                // Found start of next BB
                ret.addLast(currentBlock);
                currentBlock = new LinkedList<>();
                currentBlock.addLast(ih);
                leaders.removeFirst();
            } else {
                // Regular instruction, just add to current block
                currentBlock.addLast(ih);
            }
        }

        // Add last BB
        ret.addLast(currentBlock);

        return ret;
    }

    public Set<Pair<String, String>> start() {
        if (mg.isAbstract() || mg.isNative()) return Collections.emptySet();

        boolean includeExceptionBasicBlocks = Boolean.getBoolean("jcg.includeExceptionBasicBlocks");

        List<LinkedList<InstructionHandle>> bbs = this.computeBasicBlocks(mg.getInstructionList());

        for (LinkedList<InstructionHandle> bb : bbs) {
            if (!includeExceptionBasicBlocks && bb.getLast().getInstruction() instanceof ATHROW) {
                // skip BBs that throw exceptions
                continue;
            }

            for (InstructionHandle ih : bb) {
                Instruction i = ih.getInstruction();

                if (!visitInstruction(i)) {
                    int currentBytecodeOffset = ih.getPosition();
                    currentLineNumber = mg.getLineNumberTable(cp).getSourceLine(currentBytecodeOffset);
                    i.accept(this);
                }

            }
        }

        return methodCalls;
    }

    private boolean visitInstruction(Instruction i) {
        short opcode = i.getOpcode();
        return ((InstructionConst.getInstruction(opcode) != null)
                && !(i instanceof ConstantPushInstruction)
                && !(i instanceof ReturnInstruction));
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
        visit(i, EXPAND);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
        visit(i, EXPAND);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
        visit(i, DONT_EXPAND);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        visit(i, DONT_EXPAND);
    }

    @Override
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC i) {
        visit(i, EXPAND);
    }

    private void visit(InvokeInstruction i, Boolean shouldExpand) {
        Node caller = new Node(mg, visitedClass);
        Node receiver = new Node(i, cp, format);
        methodCalls.add(createEdge(caller.signature, receiver.signature));

        if (isTestMethod) {
            jarMetadata.testMethods.add(caller.signature);
        }

        // save the line number and method call
        jarMetadata.impliedMethodCalls.putIfAbsent(receiver.signature, new HashSet<>());
        jarMetadata
                .impliedMethodCalls
                .get(receiver.signature)
                .add(filenameAndLineNumber(visitedClass.getSourceFileName(), currentLineNumber));

        // decide if we should look at a potential expansion
        if (shouldExpand && !IGNORED_METHOD_NAMES.contains(receiver.method)) {

            // get the class types
            Optional<Class<?>> maybeReceiverType = jarMetadata.getClass(receiver.clazz);
            Optional<Class<?>> maybeCallerType = jarMetadata.getClass(caller.clazz);

            if (maybeReceiverType.isEmpty()) {
                LOGGER.error("Couldn't find Receiver class: " + receiver.clazz);
                return;
            } else if (maybeCallerType.isEmpty()) {
                LOGGER.error("Couldn't find Caller class: " + caller.clazz);
                return;
            }

            // find the method that initiated a call to another method
            Optional<Method> maybeCallingMethod =
                    jarMetadata
                            .getInspector()
                            .getTopLevelSignature(
                                    maybeCallerType.get(),
                                    MethodSignatureUtil.namedMethodSignature(
                                            caller.method, caller.argumentTypes, caller.returnType));

            if (maybeCallingMethod.isEmpty()) {
                LOGGER.error("Couldn't find top level signature for " + caller.signature);
                return;
            }

            if (maybeCallingMethod.get().isBridge()) {
                // skip the expansion if it's a bridge method
                jarMetadata.addBridgeMethod(caller.signature);
            } else {
                // record the virtual method and expand it to subtypes
                jarMetadata.addVirtualMethod(receiver.signature);
                expand(caller, receiver, maybeReceiverType.get());
            }
        }
    }

    private long expCount = 0L;

    private void expand(Node caller, Node receiver, Class<?> receiverType) {
        if (Object.class.equals(receiverType)) return;

        ClassHierarchyInspector inspector = jarMetadata.getInspector();
        expansions.putIfAbsent(receiverType, new HashMap<>());

        Set<String> exps = expansions.get(receiverType).get(receiver.method);
        if (exps == null) {
            LOGGER.info("\tExpanding to subtypes of " + receiverType.getName());
            exps =
                    jarMetadata.getReflections().getSubTypesOf(receiverType).stream()
                            .map(
                                    subtype ->
                                            inspector.getTopLevelSignature(
                                                    subtype,
                                                    MethodSignatureUtil.namedMethodSignature(
                                                            receiver.method, receiver.argumentTypes, receiver.returnType)))
                            .flatMap(Optional::stream) // Remove empty optionals
                            .map(MethodSignatureUtil::fullyQualifiedMethodSignature)
                            .collect(Collectors.toSet());

            expansions.get(receiverType).put(receiver.method, exps);
        }

        // Create a special node for this expansion
        String expNode = String.format("%s.%s - %s:%d", receiverType.getName(), receiver.method, visitedClass.getSourceFileName(), currentLineNumber);
        methodCalls.add(createEdge(caller.signature, expNode));
        jarMetadata.addCallsite(expNode);

        /* Record expanded method call */
        exps.forEach(
                expansionSignature -> {
                    methodCalls.add(createEdge(expNode, expansionSignature));
                    jarMetadata.addConcreteMethod(expansionSignature);
                });
    }

    public Pair<String, String> createEdge(String from, String to) {
        return new Pair<>(from, to);
    }

    private void recordLineNumber(Node receiver) {
        if (currentLineNumber < 0) {
            LOGGER.error(currentLineNumber + " cannot be negative!");
            System.exit(1);
        }

        throw new Error("TODO: record { class + line -> receiverSignature } in JarMetadata");
    }

    private String filenameAndLineNumber(String filename, int lineNumber) {
        return String.format("%s:%d", filename, lineNumber);
    }

    /**
     * Contains information relating to a method of a class
     *
     * <p>For internal use in {@link MethodVisitor} only, this is NOT a graph vertex.
     */
    private static class Node {
        String clazz;
        String method;
        Type[] argumentTypes;
        Type returnType;
        String signature;

        private Node(MethodGen mg, JavaClass visitedClass) {
            this.clazz = visitedClass.getClassName();
            this.method = mg.getName();
            this.argumentTypes = mg.getArgumentTypes();
            this.returnType = mg.getReturnType();
            this.signature =
                    MethodSignatureUtil.fullyQualifiedMethodSignature(
                            clazz, method, argumentTypes, returnType);
        }

        private Node(InvokeInstruction i, ConstantPoolGen cp, String format) {
            this.clazz = String.format(format, i.getReferenceType(cp));
            this.method = i.getMethodName(cp);
            this.argumentTypes = i.getArgumentTypes(cp);
            this.returnType = i.getReturnType(cp);
            this.signature =
                    MethodSignatureUtil.fullyQualifiedMethodSignature(
                            clazz, method, argumentTypes, returnType);
        }
    }
}
