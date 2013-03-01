package org.jetbrains.kannotator.classHierarchy

import java.util.*
import kotlinlib.*
import org.objectweb.asm.tree.ClassNode
import org.jetbrains.kannotator.declarations.*

private class MethodNodeImpl(data: Method): HierarchyNodeImpl<Method>() {
    // TODO Workaround for KT-2926 Bridge methods are not generated for properties declared in primary constructors
    override val data: Method = data
}

private val Method.overridable: Boolean
    get() =
        !isFinal() &&
        !isStatic() &&
        !isPrivate() &&
        !isConstructor() &&
        !isClassInitializer()

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
                val superClassNode = superClassQueue.removeFirst()
                visitedClasses.add(superClassNode)

                val superMethod = superClassNode.data.methodsById[method.id]
                val overriden =
                        superMethod != null &&
                        superMethod.overridable &&
                        (superMethod.visibility != Visibility.PACKAGE || classNode.data.name.packageName == superClassNode.data.name.packageName)
                if (overriden) {
                    getNode(method).addParentNode(getNode(superMethod!!))
                }
                else {
                    superClassQueue.addAll(superClassNode.parentNodes() - visitedClasses)
                }
            }
        }
    }

    return HierarchyGraphImpl(nodeByMethod.values(), nodeByMethod)
}