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
    val shiftForInner = if (method.isInnerClassConstructor()) 1 else 0

    val locals = localVariables.toMap { a -> a.index to a }

    val names = ArrayList<String>()
    for (index in 0..parameterSlotsInLocalVariableTable) {
        if (index < shiftForThis) continue
//        if (names.size == parameterCount - shiftForInner) break
        val local = locals[index]
        if (local != null) {
            names.add(local.name)
        }
    }

    // enums have constructors of (String, int), and local variable table mentioning only 'this'
    if (names.size < parameterCount - shiftForInner) return

    method.setParameterNames(names)
}
