package org.jetbrains.kannotator.annotationsInference.engine

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Value

public class PseudoErrorInsnNode(val srcIndex: Int): InsnNode(Opcodes.NOP) {
    override fun toString() = "Error at instruction $srcIndex"
}

fun <V: Value> pseudoErrorResult(frame: Frame<V>, srcIndex: Int): ResultFrame<V> = ResultFrame(frame, PseudoErrorInsnNode(srcIndex))