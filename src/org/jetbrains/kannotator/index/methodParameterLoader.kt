package org.jetbrains.kannotator.index

import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic

fun loadMethodParameterNames(method: Method, node: MethodNode) {
    val localVariables = node.localVariables
    if (localVariables != null) {
        val parameterCount = method.getArgumentTypes().size
        val firstIndex = if (method.isStatic()) 0 else 1
        method.setParameterNames(
                localVariables.subList(firstIndex, firstIndex + parameterCount)
                        .map { v -> v.name }
        )
    }
}

