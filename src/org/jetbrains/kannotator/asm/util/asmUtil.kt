package org.jetbrains.kannotator.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.Type
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor

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

public fun ClassReader.forEachMethod(delegateClassVisitor: ClassVisitor? = null, body: (className: String, access: Int, name: String, desc: String, signature: String?) -> Unit) {
    accept(object : ClassVisitor(Opcodes.ASM4, delegateClassVisitor) {
        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            body(getClassName(), access, name, desc, signature)
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
    }, 0)

}