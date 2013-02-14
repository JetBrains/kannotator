package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Value

public class PseudoErrorInsnNode: InsnNode(Opcodes.NOP)

fun <V: Value> pseudoErrorResult(frame: Frame<V>): ResultFrame<V> = ResultFrame(frame, PseudoErrorInsnNode())