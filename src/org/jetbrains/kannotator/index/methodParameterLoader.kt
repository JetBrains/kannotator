package org.jetbrains.kannotator.index

import org.jetbrains.kannotator.declarations.MethodWithNamedParameters
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic

fun createMethodWithNamedParameters(method: Method, node: MethodNode): MethodWithNamedParameters {
    val parameterCount = method.getArgumentTypes().size
    val firstParameterIndex = if (method.isStatic()) 0 else 1
    val localVariables = node.localVariables
    val names = if (localVariables != null) {
                    localVariables.subList(firstParameterIndex, firstParameterIndex + parameterCount).map { v -> v.name }
                }
                else {
                    (0..parameterCount - 1).map {i -> "p$i"}
                }
    return MethodWithNamedParameters(method, names)
}

