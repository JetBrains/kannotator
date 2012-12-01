package org.jetbrains.kannotator.index

import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.declarations.isInnerClassConstructor
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.isConstructor
import kotlinlib.suffixAfter
import org.jetbrains.kannotator.declarations.isAnonymous
import kotlinlib.toMap
import org.objectweb.asm.Type

val NO_PARAMETER_NAME: String = "<no name>"

fun loadMethodParameterNames(method: Method, node: MethodNode) {
    // inner class constructors take the closure in the form of parameters
    // the corresponding variable table may have some funny relation to these parameters
    if (method.isConstructor() && method.declaringClass.isAnonymous()) return

    val localVariables = node.localVariables
    if (localVariables == null || localVariables.isEmpty()) return

    val parameterTypes = method.getArgumentTypes()
    val parameterCount = parameterTypes.size

    // Longs and Doubles occupy two slots in the local vars table
    var parameterSlotsInLocalVariableTable = parameterCount
    for (t in parameterTypes) {
        if (t.getSort() == Type.LONG || t.getSort() == Type.DOUBLE) {
            parameterSlotsInLocalVariableTable++
        }
    }

    val shiftForThis = if (method.isStatic()) 0 else 1

    val locals = localVariables.toMap { a -> a.index to a }

    val names = ArrayList<String>()
    var index = shiftForThis
    for (paramType in parameterTypes) {
        val local = locals[index]
        if (local != null) {
            names.add(local.name)
        }
        else {
            names.add(NO_PARAMETER_NAME)
        }

        index += when (paramType.getSort()) {
            Type.LONG,
            Type.DOUBLE -> 2
            else -> 1
        }
    }

    method.setParameterNames(names)
    System.err?.println(names)
}
