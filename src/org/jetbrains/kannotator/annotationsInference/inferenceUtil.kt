package org.jetbrains.kannotator.annotationsInference

import kotlinlib.emptySet
import org.jetbrains.kannotator.asm.util.getArgumentCount
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.kannotator.declarations.Field
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.Type
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType

trait Annotation

public fun <A: Annotation> generateAssertsForCallArguments(
        instructionNode: AbstractInsnNode?,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<A>,
        addAssertForStackValue: (Int) -> Unit,
        needGenerateAssertForThis: Boolean,
        needGenerateAssertForArgument: (A?) -> Boolean,
        addAssertForExternalStackValue: (Int) -> Unit
) {
    if (instructionNode !is MethodInsnNode) throw IllegalArgumentException("Not a method instruction: $instructionNode")
    val hasThis = instructionNode.getOpcode() != INVOKESTATIC
    val thisSlots = if (hasThis) 1 else 0
    val parametersCount = instructionNode.getArgumentCount() + thisSlots
    val argTypes = Type.getArgumentTypes(instructionNode.desc)

    fun addAssertForArgumentOnStack(index: Int, external: Boolean) {
        if (index < thisSlots || !argTypes[index - thisSlots].isPrimitiveOrVoidType()) {
            val indexFromTop = parametersCount - index - 1
            if (external)
                addAssertForExternalStackValue(indexFromTop)
            else
                addAssertForStackValue(indexFromTop)
        }
    }

    if (hasThis && needGenerateAssertForThis) {
        addAssertForArgumentOnStack(0, false)
    }
    if (instructionNode.getOpcode() != INVOKEDYNAMIC) {
        val method = declarationIndex.findMethodByMethodInsnNode(instructionNode)
        if (method != null) {
            val positions = PositionsForMethod(method)
            for (paramIndex in thisSlots..parametersCount - 1) {
                val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                if (paramAnnotation != null) {
                    if (needGenerateAssertForArgument(paramAnnotation)) {
                        addAssertForArgumentOnStack(paramIndex, false)
                    }
                } else {
                    addAssertForArgumentOnStack(paramIndex, true)
                }
            }
            return
        }
    }

    for (paramIndex in thisSlots..parametersCount - 1) {
        addAssertForArgumentOnStack(paramIndex, true)
    }
}

fun DeclarationIndex.findMethodByMethodInsnNode(methodInsnNode: MethodInsnNode): Method? {
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

fun DeclarationIndex.findFieldByFieldInsnNode(fieldInsnNode: FieldInsnNode): Field? {
    return this.findField(ClassName.fromInternalName(fieldInsnNode.owner), fieldInsnNode.name)
}
