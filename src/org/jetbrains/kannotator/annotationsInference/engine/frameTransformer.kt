package org.jetbrains.kannotator.annotationsInference.engine

import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.Collections
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.VarInsnNode
import java.util.ArrayList

public enum class EdgeKind {
    DEFAULT
    FALSE
    TRUE
    EXCEPTION
}

public trait FrameTransformer<V: Value> {
    /**
     * Returns collection of results for pseudo-transitions with their frames for given instruction and pre-frame
     * @param insnNode current instruction
     * @param preFrame pre-frame of insnNode instruction
     * @param executedFrame frame produced from pre-frame by insnNode execution
     * @return collection of results for pseudo-transitions
     */
    public fun getPseudoResults(
            insnNode: AbstractInsnNode,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Collection<ResultFrame<V>> = Collections.emptyList()

    /**
     * Returns post-frame for given instruction and pre-frame. If post-frame is unreachable, method must return null
     * @param insnNode current instruction
     * @param edgeIndex zero-based index of the edge starting from current instruction
     * @param preFrame pre-frame of insnNode instruction
     * @param executedFrame frame produced from pre-frame by insnNode execution
     * @return post-frame if it's reachable and null, otherwise
     */
    public fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeKind: EdgeKind,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Frame<V>? = executedFrame
}

public open class DefaultFrameTransformer<V: Value>: FrameTransformer<V> {
    public override fun getPseudoResults(
            insnNode: AbstractInsnNode,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Collection<ResultFrame<V>> = Collections.emptyList()

    public override fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeKind: EdgeKind,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Frame<V>? = executedFrame
}