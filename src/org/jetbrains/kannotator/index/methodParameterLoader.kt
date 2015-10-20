package org.jetbrains.kannotator.index

import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic
import java.util.ArrayList
import java.util.LinkedHashMap
import org.objectweb.asm.tree.LocalVariableNode

val NO_PARAMETER_NAME: String = "<no name>"

fun loadMethodParameterNames(method: Method, node: MethodNode) {
    // java 6 for inner class constructor give this. java 8 -- this and this$0
    val localVariables = node.localVariables?.filter { it.name != "this$0" }
    if (localVariables == null || localVariables.isEmpty()) return

    val parameterTypes = method.getArgumentTypes()
    val locals = LinkedHashMap<Int, LocalVariableNode>(localVariables.size)
    for (local in localVariables) {
        locals.put(local.index, local)
    }

    val names = ArrayList<String>()
    var index = if (method.isStatic()) 0 else 1
    for (paramType in parameterTypes) {
        val local = locals[index]

        names.add(local?.name ?: NO_PARAMETER_NAME)

        index += paramType.size
    }

    method.setParameterNames(names)
}
