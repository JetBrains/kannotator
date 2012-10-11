package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.*
import kotlinlib.prefixUpToLast

private val Method.isInheritable: Boolean
    get() =
        !isFinal() &&
        !isStatic() &&
        visibility != Visibility.PRIVATE &&
        id.methodName != "<init>" &&
        id.methodName != "<clinit>"

fun samePackage(c1: ClassData, c2: ClassData): Boolean {
    fun ClassData._package(): String = name.internal.prefixUpToLast('/')!!
    return c1._package() == c2._package()
}

fun HierarchyNode<ClassData>.getOverridingMethods(method: Method): Set<Method> {
    val my = data.methodsById[method.id]
    if (my == null) return hashSet()

    val result = hashSet<Method>(my)

    if (!my.isInheritable) return result

    for (subClassEdge in children) {
        val subClass = subClassEdge.child
        if (my.visibility == Visibility.PACKAGE && !samePackage(this.data, subClass.data)) {
            continue
        }
        result.addAll(subClass.getOverridingMethods(method))
    }

    return result
}
