package org.jetbrains.kannotator.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LabelNode

public fun AbstractInsnNode.toOpcodeString(): String {
    return when (this) {
        is LineNumberNode -> "Line " + line
        is LabelNode -> "Label: ${getLabel()}"
        else -> {
            val opcode = getOpcode()
            if (opcode == -1) {
                "No opcode: " + javaClass.getSimpleName()
            }
            else {
                Printer.OPCODES[opcode]!!
            }
        }
    }
}
