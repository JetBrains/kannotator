package org.jetbrains.kannotator.index

import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.declarations.isInnerClassConstructor

fun loadMethodParameterNames(method: Method, node: MethodNode) {
    val localVariables = node.localVariables
    if (localVariables == null || localVariables.isEmpty()) return

    val parameterCount = method.getArgumentTypes().size
    val shiftForThis = if (method.isStatic()) 0 else 1
    val shiftForInner = if (method.isInnerClassConstructor()) 1 else 0
    val toIndex = shiftForThis + parameterCount - shiftForInner
    method.setParameterNames(
            localVariables.subList(shiftForThis, toIndex)
                    .map { v -> v.name }
    )
}

