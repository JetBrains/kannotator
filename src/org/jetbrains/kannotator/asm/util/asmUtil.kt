package org.jetbrains.kannotator.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Printer

public fun AbstractInsnNode.toOpcodeString(): String {
    val opcode = getOpcode()
    if (opcode == -1) {
        return "No opcode: " + javaClass.getSimpleName()
    }
    return Printer.OPCODES[opcode]!!
}
