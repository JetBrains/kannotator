package org.jetbrains.kannotator.classHierarchy

import java.util.*
import kotlinlib.*
import org.objectweb.asm.tree.ClassNode
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.graphs.*

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

class MethodHierarchyBuilder: GraphBuilderImpl<Method, Method, Any?>(true, true) {
    override fun newGraph(): GraphImpl<Method, Any?> = HierarchyGraphImpl(createNodeMap)

    override fun newNode(method: Method): MethodNodeImpl = MethodNodeImpl(method)

    override fun newEdge(label: Any?, from: NodeImpl<Method, Any?>, to: NodeImpl<Method, Any?>): HierarchyEdgeImpl<Method> =
            HierarchyEdgeImpl(from as MethodNodeImpl, to as MethodNodeImpl)
}

fun buildMethodHierarchy(classHierarchy: HierarchyGraph<ClassData>): HierarchyGraph<Method> {
    val builder = MethodHierarchyBuilder()

    for (classNode in classHierarchy.nodes) {
        for (method in classNode.methods) {
            val superClassQueue = LinkedHashSet((classNode as ClassNodeImpl).parentNodes)
            val visitedClasses = HashSet<ClassNodeImpl>()

            while (!superClassQueue.isEmpty()) {
                val superClassNode = superClassQueue.removeFirst()
                visitedClasses.add(superClassNode as ClassNodeImpl)

                val superMethod = superClassNode.data.methodsById[method.id]
                val overriden =
                        superMethod != null &&
                        superMethod.overridable &&
                        (superMethod.visibility != Visibility.PACKAGE || classNode.data.name.packageName == superClassNode.data.name.packageName)
                if (overriden) {
                    builder.getOrCreateEdge(null, builder.getOrCreateNode(superMethod!!), builder.getOrCreateNode(method))
                }
                else {
                    superClassQueue.addAll(superClassNode.parentNodes - visitedClasses)
                }
            }
        }
    }

    return builder.toGraph() as HierarchyGraph<Method>
}