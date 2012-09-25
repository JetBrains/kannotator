package org.jetbrains.kannotator.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.Type

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

public fun Type.isPrimitiveOrVoidType() : Boolean {
    val sort = this.getSort()
    return sort == Type.VOID || sort == Type.BOOLEAN || sort == Type.CHAR || sort == Type.BYTE ||
            sort == Type.SHORT || sort == Type.INT || sort == Type.FLOAT || sort == Type.LONG ||
            sort == Type.DOUBLE;
}
