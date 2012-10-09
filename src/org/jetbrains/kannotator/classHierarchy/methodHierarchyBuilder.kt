package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.Method
import java.util.HashMap
import java.util.LinkedList
import java.util.LinkedHashSet
import kotlinlib.*
import org.objectweb.asm.tree.ClassNode
import java.util.HashSet
import org.jetbrains.kannotator.declarations.ClassName

private class MethodNodeImpl(data: Method): HierarchyNodeImpl<Method>() {
    // TODO Workaround for KT-2926 Bridge methods are not generated for properties declared in primary constructors
    override val data: Method = data
}

fun buildMethodHierarchy(classHierarchy: HierarchyGraph<ClassData>): HierarchyGraph<Method> {
    val nodeByMethod = HashMap<Method, MethodNodeImpl>()

    fun getNode(method: Method): HierarchyNodeImpl<Method> {
        return nodeByMethod.getOrPut(method) { MethodNodeImpl(method) }
    }

    for (classNode in classHierarchy.nodes) {
        for (method in classNode.methods) {

            val superClassQueue = LinkedHashSet(classNode.parentNodes())
            val visitedClasses = HashSet<HierarchyNode<ClassData>>()

            while (!superClassQueue.isEmpty()) {
                val superClass = superClassQueue.removeFirst()
                visitedClasses.add(superClass)

                val superMethod = superClass.data.methodsById[method.id]
                if (superMethod != null) {
                    getNode(method).addParentNode(getNode(superMethod))
                }
                else {
                    superClassQueue.addAll(superClass.parentNodes() - visitedClasses)
                }
            }
        }
    }

    return HierarchyGraphImpl(nodeByMethod.values())
}