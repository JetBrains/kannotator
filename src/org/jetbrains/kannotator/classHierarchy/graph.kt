package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.*

trait ClassHierarchyGraph {
    val classes: Collection<ClassNode>
}

trait ClassHierarchyEdge {
    val base: ClassNode
    val derived: ClassNode
}

trait ClassNode {
    val subClasses: Collection<ClassHierarchyEdge>
    val superClasses: Collection<ClassHierarchyEdge>

    val name: ClassName

    val methods: Set<Method>
}



private val Method.isInheritable: Boolean
    get() =
        !isFinal() &&
        !isStatic() &&
        visibility != Visibility.PRIVATE &&
        id.methodName != "<init>" &&
        id.methodName != "<clinit>"

fun ClassNode.find(method: Method): Method? = methods.find { it.id == method.id }

fun ClassNode.getOverriddenMethods(method: Method): Set<Method> {
    val my = find(method)
    if (my == null) return hashSet()

    val result = hashSet<Method>(my)

    if (!my.isInheritable) return result

    for (subClass in subClasses) {
        result.addAll(subClass.derived.getOverriddenMethods(method))
    }

    return result
}
