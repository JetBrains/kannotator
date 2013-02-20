package org.jetbrains.kannotator.annotationsInference.engine

import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.AbstractInsnNode

public data class AnalysisResult<V: Value>(
        val mergedFrames: Map<AbstractInsnNode, Frame<V>>,
        val returnInstructions: Set<AbstractInsnNode>,
        val errorInstructions: Set<AbstractInsnNode>
)
