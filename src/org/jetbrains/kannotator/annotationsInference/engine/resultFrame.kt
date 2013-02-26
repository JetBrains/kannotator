package org.jetbrains.kannotator.annotationsInference.engine

import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.Collections

public data class ResultFrame<V: Value>(val frame: Frame<V>, val insnNode: AbstractInsnNode)

fun <V: Value> singletonOrEmptyResult(frame: Frame<V>?, insnIndex: Int): Collection<ResultFrame<V>> {
    return if (frame != null)
        Collections.singletonList(pseudoErrorResult(frame, insnIndex))
    else
        Collections.emptyList<ResultFrame<V>>()
}