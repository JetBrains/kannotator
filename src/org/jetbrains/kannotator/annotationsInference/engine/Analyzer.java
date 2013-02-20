/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * Copyright (c) 2011 Google
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jetbrains.kannotator.annotationsInference.engine;

import jet.runtime.typeinfo.KotlinSignature;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

/**
 * A semantic bytecode analyzer. <i>This class does not fully check that JSR and
 * RET instructions are valid.</i>
 *
 * @param <V> type of the Value used for the analysis.
 *
 *  @author Eric Bruneton
 */
public class Analyzer<V extends Value> implements Opcodes {
    private MethodNode m;

    private final Interpreter<V> interpreter;

    private final FrameTransformer<V> frameTransformer;

    private int n;

    private InsnList insns;

    private List<TryCatchBlockNode>[] handlers;

    private Frame<V>[] frames;

    private Map<AbstractInsnNode, Frame<V>> frameMap;

    private Set<AbstractInsnNode> errorInstructions;

    private Set<AbstractInsnNode> returnInstructions;

    private Subroutine[] subroutines;

    private boolean[] queued;

    private int[] queue;

    private int top;

    /**
     * Constructs a new {@link Analyzer}.
     *
     * @param interpreter the interpreter to be used to symbolically interpret
     *        the bytecode instructions.
     * @param frameTransformer the frame transformer to be used to construct post-frames and pseudo-error frames
     */
    public Analyzer(
            final Interpreter<V> interpreter,
            final FrameTransformer<V> frameTransformer) {
        this.interpreter = interpreter;
        this.frameTransformer = frameTransformer;
    }

    /**
     * Constructs a new {@link Analyzer}.
     *
     * @param interpreter the interpreter to be used to symbolically interpret
     *        the bytecode instructions.
     */
    public Analyzer(
            final Interpreter<V> interpreter) {
        this(interpreter, new DefaultFrameTransformer<V>());
    }

    /**
     * Analyzes the given method.
     *
     * @param owner the internal name of the class to which the method belongs.
     * @param methodNode the method to be analyzed.
     * @return the symbolic state of the execution stack frame at each bytecode
     *         instruction of the method. The size of the returned array is
     *         equal to the number of instructions (and labels) of the method. A
     *         given frame is <tt>null</tt> if and only if the corresponding
     *         instruction cannot be reached (dead code).
     * @throws AnalyzerException if a problem occurs during the analysis.
     */

    @KotlinSignature("fun analyze(owner : String, methodNode : MethodNode) : AnalysisResult<V>")
    @SuppressWarnings("unchecked")
    public AnalysisResult<V> analyze(final String owner, final MethodNode methodNode)
            throws AnalyzerException
    {
        m = methodNode;
        returnInstructions = new HashSet<AbstractInsnNode>();
        errorInstructions = new HashSet<AbstractInsnNode>();
        frameMap = new HashMap();

        if ((m.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
            frames = (Frame<V>[])new Frame<?>[0];
            return new AnalysisResult<V>(frameMap, returnInstructions, errorInstructions);
        }

        n = m.instructions.size();
        frames = (Frame<V>[])new Frame<?>[n];

        insns = m.instructions;

        handlers = (List<TryCatchBlockNode>[])new List<?>[n];
        subroutines = new Subroutine[n];

        queued = new boolean[n];
        queue = new int[n];
        top = 0;

        // computes exception handlers for each instruction
        for (int i = 0; i < m.tryCatchBlocks.size(); ++i) {
            TryCatchBlockNode tcb = m.tryCatchBlocks.get(i);
            int begin = insns.indexOf(tcb.start);
            int end = insns.indexOf(tcb.end);
            for (int j = begin; j < end; ++j) {
                List<TryCatchBlockNode> insnHandlers = handlers[j];
                if (insnHandlers == null) {
                    insnHandlers = new ArrayList<TryCatchBlockNode>();
                    handlers[j] = insnHandlers;
                }
                insnHandlers.add(tcb);
            }
        }

        // computes the subroutine for each instruction:
        Subroutine main = new Subroutine(null, m.maxLocals, null);
        List<AbstractInsnNode> subroutineCalls = new ArrayList<AbstractInsnNode>();
        Map<LabelNode, Subroutine> subroutineHeads = new HashMap<LabelNode, Subroutine>();
        findSubroutine(0, main, subroutineCalls);
        while (!subroutineCalls.isEmpty()) {
            JumpInsnNode jsr = (JumpInsnNode) subroutineCalls.remove(0);
            Subroutine sub = subroutineHeads.get(jsr.label);
            if (sub == null) {
                sub = new Subroutine(jsr.label, m.maxLocals, jsr);
                subroutineHeads.put(jsr.label, sub);
                findSubroutine(insns.indexOf(jsr.label), sub, subroutineCalls);
            } else {
                sub.callers.add(jsr);
            }
        }
        for (int i = 0; i < n; ++i) {
            if (subroutines[i] != null && subroutines[i].start == null) {
                subroutines[i] = null;
            }
        }

        // initializes the data structures for the control flow analysis
        Frame<V> current = newFrame(m.maxLocals, m.maxStack);
        Frame<V> handler = newFrame(m.maxLocals, m.maxStack);
        Type returnType = Type.getReturnType(m.desc);
        current.setReturn(returnType != Type.VOID_TYPE ? interpreter.newValue(returnType) : null);
        Type[] args = Type.getArgumentTypes(m.desc);
        int local = 0;
        if ((m.access & ACC_STATIC) == 0) {
            Type ctype = Type.getObjectType(owner);
            current.setLocal(local++, interpreter.newValue(ctype));
        }
        for (int i = 0; i < args.length; ++i) {
            current.setLocal(local++, interpreter.newValue(args[i]));
            if (args[i].getSize() == 2) {
                current.setLocal(local++, interpreter.newValue(null));
            }
        }
        while (local < m.maxLocals) {
            current.setLocal(local++, interpreter.newValue(null));
        }
        merge(0, current, null);

        init(owner, m);

        // control flow analysis
        while (top > 0) {
            int insn = queue[--top];
            Frame<V> f = frames[insn];
            Subroutine subroutine = subroutines[insn];
            queued[insn] = false;

            AbstractInsnNode insnNode = null;
            try {
                insnNode = m.instructions.get(insn);
                int insnOpcode = insnNode.getOpcode();
                int insnType = insnNode.getType();

                if (insnType == AbstractInsnNode.LABEL
                        || insnType == AbstractInsnNode.LINE
                        || insnType == AbstractInsnNode.FRAME)
                {
                    merge(insn + 1, f, subroutine);
                    newControlFlowEdge(insn, insn + 1, newFrame(current));
                } else {
                    current.init(f).execute(insnNode, interpreter);
                    for (ResultFrame<V> pseudoResult : frameTransformer.getPseudoResults(insnNode, f, current, this)) {
                        if (pseudoResult.getInsnNode() instanceof PseudoErrorInsnNode) {
                            errorInstructions.add(pseudoResult.getInsnNode());
                            frameMap.put(pseudoResult.getInsnNode(), pseudoResult.getFrame());
                        }
                    }
                    subroutine = subroutine == null ? null : subroutine.copy();

                    Frame<V> postFrame;

                    if (insnNode instanceof JumpInsnNode) {
                        JumpInsnNode j = (JumpInsnNode) insnNode;
                        if (insnOpcode != GOTO && insnOpcode != JSR) {
                            postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.FALSE, f, current, this);
                            merge(insn + 1, postFrame, subroutine);
                            if (postFrame != null) {
                                newControlFlowEdge(insn, insn + 1, newFrame(postFrame));
                            }
                        }

                        int jump = insns.indexOf(j.label);
                        if (insnOpcode == JSR) {
                            postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                            merge(
                                    jump,
                                    postFrame,
                                    new Subroutine(j.label, m.maxLocals, j)
                            );
                        } else {
                            EdgeKind edgeKind = insnOpcode == GOTO ? EdgeKind.DEFAULT : EdgeKind.TRUE;
                            postFrame = frameTransformer.getPostFrame(insnNode, edgeKind, f, current, this);
                            merge(jump, postFrame, subroutine);
                        }
                        if (postFrame != null) {
                            newControlFlowEdge(insn, jump, newFrame(postFrame));
                        }
                    } else if (insnNode instanceof LookupSwitchInsnNode) {
                        LookupSwitchInsnNode lsi = (LookupSwitchInsnNode) insnNode;
                        int jump = insns.indexOf(lsi.dflt);
                        postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                        merge(jump, postFrame, subroutine);
                        if (postFrame != null) {
                            newControlFlowEdge(insn, jump, newFrame(postFrame));
                        }
                        for (int j = 0; j < lsi.labels.size(); ++j) {
                            LabelNode label = lsi.labels.get(j);
                            jump = insns.indexOf(label);
                            postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                            merge(jump, postFrame, subroutine);
                            if (postFrame != null) {
                                newControlFlowEdge(insn, jump, newFrame(postFrame));
                            }
                        }
                    } else if (insnNode instanceof TableSwitchInsnNode) {
                        TableSwitchInsnNode tsi = (TableSwitchInsnNode) insnNode;
                        int jump = insns.indexOf(tsi.dflt);
                        postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                        merge(jump, postFrame, subroutine);
                        if (postFrame != null) {
                            newControlFlowEdge(insn, jump, newFrame(postFrame));
                        }
                        for (int j = 0; j < tsi.labels.size(); ++j) {
                            LabelNode label = tsi.labels.get(j);
                            jump = insns.indexOf(label);
                            postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                            merge(jump, postFrame, subroutine);
                            if (postFrame != null) {
                                newControlFlowEdge(insn, jump, newFrame(postFrame));
                            }
                        }
                    } else if (insnOpcode == RET) {
                        if (subroutine == null) {
                            throw new AnalyzerException(insnNode, "RET instruction outside of a sub routine");
                        }
                        for (int i = 0; i < subroutine.callers.size(); ++i) {
                            JumpInsnNode caller = subroutine.callers.get(i);
                            int call = insns.indexOf(caller);
                            if (frames[call] != null) {
                                postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                                merge(call + 1,
                                        frames[call],
                                        postFrame,
                                        subroutines[call],
                                        subroutine.access);
                                if (postFrame != null) {
                                    newControlFlowEdge(insn, call + 1, newFrame(postFrame));
                                }
                            }
                        }
                    } else if (insnOpcode != ATHROW
                            && (insnOpcode < IRETURN || insnOpcode > RETURN))
                    {
                        if (subroutine != null) {
                            if (insnNode instanceof VarInsnNode) {
                                int var = ((VarInsnNode) insnNode).var;
                                subroutine.access[var] = true;
                                if (insnOpcode == LLOAD || insnOpcode == DLOAD
                                        || insnOpcode == LSTORE
                                        || insnOpcode == DSTORE)
                                {
                                    subroutine.access[var + 1] = true;
                                }
                            } else if (insnNode instanceof IincInsnNode) {
                                int var = ((IincInsnNode) insnNode).var;
                                subroutine.access[var] = true;
                            }
                        }
                        postFrame = frameTransformer.getPostFrame(insnNode, EdgeKind.DEFAULT, f, current, this);
                        merge(insn + 1, postFrame, subroutine);
                        if (postFrame != null) {
                            newControlFlowEdge(insn, insn + 1, newFrame(postFrame));
                        }
                    }
                }

                List<TryCatchBlockNode> insnHandlers = handlers[insn];
                if (insnHandlers != null) {
                    for (int i = 0; i < insnHandlers.size(); ++i) {
                        TryCatchBlockNode tcb = insnHandlers.get(i);
                        Type type;
                        if (tcb.type == null) {
                            type = Type.getObjectType("java/lang/Throwable");
                        } else {
                            type = Type.getObjectType(tcb.type);
                        }
                        int jump = insns.indexOf(tcb.handler);
                        if (isInterestingControlFlowExceptionEdge(insn, tcb)) {
                            handler.init(f);
                            handler.clearStack();
                            handler.push(interpreter.newValue(type));
                            newControlFlowExceptionEdge(insn, tcb, newFrame(handler));
                            merge(jump, frameTransformer.getPostFrame(insnNode, EdgeKind.EXCEPTION, f, handler, this), subroutine);
                        }
                    }
                }
            } catch (AnalyzerException e) {
                throw new AnalyzerException(e.node, "Error at instruction " + insn
                        + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new AnalyzerException(insnNode, "Error at instruction " + insn
                        + ": " + e.getMessage(), e);
            }
        }

        for (int i = 0; i < n; i++) {
            if (frames[i] == null) {
                continue;
            }

            AbstractInsnNode insnNode = m.instructions.get(i);
            int opcode = insnNode.getOpcode();

            if (opcode == ATHROW) {
                errorInstructions.add(insnNode);
            } else if (opcode >= IRETURN && opcode <= RETURN) {
                returnInstructions.add(insnNode);
            }

            frameMap.put(insnNode, frames[i]);
        }

        return new AnalysisResult<V>(frameMap, returnInstructions, errorInstructions);
    }

    private void findSubroutine(int insn, final Subroutine sub, final List<AbstractInsnNode> calls)
            throws AnalyzerException
    {
        while (true) {
            if (insn < 0 || insn >= n) {
                throw new AnalyzerException(null, "Execution can fall off end of the code");
            }
            if (subroutines[insn] != null) {
                return;
            }
            subroutines[insn] = sub.copy();
            AbstractInsnNode node = insns.get(insn);

            // calls findSubroutine recursively on normal successors
            if (node instanceof JumpInsnNode) {
                if (node.getOpcode() == JSR) {
                    // do not follow a JSR, it leads to another subroutine!
                    calls.add(node);
                } else {
                    JumpInsnNode jnode = (JumpInsnNode) node;
                    findSubroutine(insns.indexOf(jnode.label), sub, calls);
                }
            } else if (node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tsnode = (TableSwitchInsnNode) node;
                findSubroutine(insns.indexOf(tsnode.dflt), sub, calls);
                for (int i = tsnode.labels.size() - 1; i >= 0; --i) {
                    LabelNode l = tsnode.labels.get(i);
                    findSubroutine(insns.indexOf(l), sub, calls);
                }
            } else if (node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lsnode = (LookupSwitchInsnNode) node;
                findSubroutine(insns.indexOf(lsnode.dflt), sub, calls);
                for (int i = lsnode.labels.size() - 1; i >= 0; --i) {
                    LabelNode l = lsnode.labels.get(i);
                    findSubroutine(insns.indexOf(l), sub, calls);
                }
            }

            // calls findSubroutine recursively on exception handler successors
            List<TryCatchBlockNode> insnHandlers = handlers[insn];
            if (insnHandlers != null) {
                for (int i = 0; i < insnHandlers.size(); ++i) {
                    TryCatchBlockNode tcb = insnHandlers.get(i);
                    findSubroutine(insns.indexOf(tcb.handler), sub, calls);
                }
            }

            // if insn does not falls through to the next instruction, return.
            switch (node.getOpcode()) {
                case GOTO:
                case RET:
                case TABLESWITCH:
                case LOOKUPSWITCH:
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case DRETURN:
                case ARETURN:
                case RETURN:
                case ATHROW:
                    return;
            }
            insn++;
        }
    }

    /**
     * Returns current method node
     * @return Method node
     */
    public MethodNode getMethodNode() {
        return m;
    }

    /**
     * Returns the symbolic stack frame for each instruction of the last
     * recently analyzed method.
     *
     * @return the symbolic state of the execution stack frame at each bytecode
     *         instruction of the method. The size of the returned array is
     *         equal to the number of instructions (and labels) of the method. A
     *         given frame is <tt>null</tt> if the corresponding instruction
     *         cannot be reached, or if an error occured during the analysis of
     *         the method.
     */
    @KotlinSignature("fun getFrames() : Array<out Frame<V>?>")
    public Frame<V>[] getFrames() {
        return frames;
    }

    /**
     * Returns the exception handlers for the given instruction.
     *
     * @param insn the index of an instruction of the last recently analyzed
     *        method.
     * @return a list of {@link TryCatchBlockNode} objects.
     */
    public List<TryCatchBlockNode> getHandlers(final int insn) {
        return handlers[insn];
    }

    /**
     * Initializes this analyzer. This method is called just before the
     * execution of control flow analysis loop in #analyze. The default
     * implementation of this method does nothing.
     *
     * @param owner the internal name of the class to which the method belongs.
     * @param m the method to be analyzed.
     * @throws AnalyzerException if a problem occurs.
     */
    protected void init(String owner, MethodNode m) throws AnalyzerException {
    }

    /**
     * Constructs a new frame with the given size.
     *
     * @param nLocals the maximum number of local variables of the frame.
     * @param nStack the maximum stack size of the frame.
     * @return the created frame.
     */
    protected Frame<V> newFrame(final int nLocals, final int nStack) {
        return new Frame<V>(nLocals, nStack);
    }

    /**
     * Constructs a new frame that is identical to the given frame.
     *
     * @param src a frame.
     * @return the created frame.
     */
    @KotlinSignature("fun newFrame(src : Frame<out V>) : Frame<V>")
    protected Frame<V> newFrame(final Frame<? extends V> src) {
        return new Frame<V>(src);
    }

    /**
     * Creates a control flow graph edge. The default implementation of this
     * method does nothing. It can be overriden in order to construct the
     * control flow graph of a method (this method is called by the
     * {@link #analyze analyze} method during its visit of the method's code).
     *
     * @param insn an instruction index.
     * @param successor index of a successor instruction.
     */
    @KotlinSignature("fun newControlFlowEdge(insn : Int, successor : Int, frame : Frame<V>) : Unit")
    protected void newControlFlowEdge(final int insn, final int successor, Frame<V> frame) {
    }

    /**
     * Creates a control flow graph edge corresponding to an exception handler.
     * The default implementation of this method does nothing. It can be
     * overridden in order to construct the control flow graph of a method (this
     * method is called by the {@link #analyze analyze} method during its visit
     * of the method's code).
     *
     * @param insn an instruction index.
     * @param successor index of a successor instruction.
     * @param frame copy of the current frame
     */
    @KotlinSignature("fun newControlFlowExceptionEdge(insn : Int, successor : Int, frame : Frame<V>) : Unit")
    protected void newControlFlowExceptionEdge(final int insn, final int successor, Frame<V> frame) {
    }

    /**
     * @param insn an instruction index.
     * @param successor index of a successor instruction.
     * @return true if this edge must be considered in the data flow analysis
     *         performed by this analyzer, or false otherwise. The default
     *         implementation of this method always returns true.
     */
    protected boolean isInterestingControlFlowExceptionEdge(final int insn, final int successor)
    {
        return true;
    }

    /**
     * Creates a control flow graph edge corresponding to an exception handler.
     * The default implementation of this method delegates to
     * {@link #newControlFlowExceptionEdge(int, int, Frame)
     * newControlFlowExceptionEdge(int, int)}. It can be overridden in order to
     * construct the control flow graph of a method (this method is called by
     * the {@link #analyze analyze} method during its visit of the method's
     * code).
     *
     * @param insn an instruction index.
     * @param tcb TryCatchBlockNode corresponding to this edge.
     * @param frame copy of the current frame
     */
    @KotlinSignature("fun newControlFlowExceptionEdge(insn : Int, tcb : TryCatchBlockNode?, frame : Frame<V>) : Unit")
    protected void newControlFlowExceptionEdge(final int insn, final TryCatchBlockNode tcb, Frame<V> frame)
    {
        newControlFlowExceptionEdge(insn, insns.indexOf(tcb.handler), frame);
    }

    /**
     * @param insn an instruction index.
     * @param tcb TryCatchBlockNode corresponding to this edge.
     * @return true if this edge must be considered in the data flow analysis
     *         performed by this analyzer, or false otherwise. The default
     *         implementation of this method delegates to
     *         {@link #isInterestingControlFlowExceptionEdge(int, int)}
     *         newControlFlowExceptionEdge(int, int)}.
     */
    protected boolean isInterestingControlFlowExceptionEdge(final int insn, final TryCatchBlockNode tcb)
    {
        return isInterestingControlFlowExceptionEdge(insn, insns.indexOf(tcb.handler));
    }

    // -------------------------------------------------------------------------

    private void merge(
            final int insn,
            final Frame<V> frame,
            final Subroutine subroutine) throws AnalyzerException
    {
        if (frame == null) {
            return;
        }

        Frame<V> oldFrame = frames[insn];
        Subroutine oldSubroutine = subroutines[insn];
        boolean changes;

        if (oldFrame == null) {
            frames[insn] = newFrame(frame);
            changes = true;
        } else {
            changes = oldFrame.merge(frame, interpreter);
        }

        if (oldSubroutine == null) {
            if (subroutine != null) {
                subroutines[insn] = subroutine.copy();
                changes = true;
            }
        } else {
            if (subroutine != null) {
                changes |= oldSubroutine.merge(subroutine);
            }
        }
        if (changes && !queued[insn]) {
            queued[insn] = true;
            queue[top++] = insn;
        }
    }

    private void merge(
            final int insn,
            final Frame<V> beforeJSR,
            final Frame<V> afterRET,
            final Subroutine subroutineBeforeJSR,
            final boolean[] access) throws AnalyzerException
    {
        Frame<V> oldFrame = frames[insn];
        Subroutine oldSubroutine = subroutines[insn];
        boolean changes;

        afterRET.merge(beforeJSR, access);

        if (oldFrame == null) {
            frames[insn] = newFrame(afterRET);
            changes = true;
        } else {
            changes = oldFrame.merge(afterRET, interpreter);
        }

        if (oldSubroutine != null && subroutineBeforeJSR != null) {
            changes |= oldSubroutine.merge(subroutineBeforeJSR);
        }
        if (changes && !queued[insn]) {
            queued[insn] = true;
            queue[top++] = insn;
        }
    }
}