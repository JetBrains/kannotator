package org.jetbrains.kannotator.asm.util

import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.objectweb.asm.FieldVisitor

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

public fun ClassReader.forEachField(
        delegateClassVisitor: ClassVisitor? = null,
        body: (className: String, access: Int, name: String, desc: String, signature: String?, value: Any?) -> Unit) {
    accept(object : ClassVisitor(Opcodes.ASM4, delegateClassVisitor) {
        public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
            body(getClassName(), access, name, desc, signature, value)
            return super.visitField(access, name, desc, signature, value)
        }
    }, 0)
}

public fun Instruction.getAsmInstructionNode(): AbstractInsnNode? =
        (this.metadata as? AsmInstructionMetadata)?.asmInstruction

public fun Instruction.getOpcode(): Int? = getAsmInstructionNode()?.getOpcode()

public fun ClassReader.forEachMethodWithMethodVisitor(body: (className: String, access: Int, name: String, desc: String, signature: String?) -> MethodVisitor?) {
    accept(object : ClassVisitor(Opcodes.ASM4) {
        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return body(getClassName(), access, name, desc, signature)
        }
    }, 0)

}

public fun Method.createMethodNode(): MethodNode = MethodNode(access.flags, id.methodName, id.methodDesc, genericSignature, null)

public fun MethodInsnNode.getArgumentCount(): Int = Type.getArgumentTypes(desc).size