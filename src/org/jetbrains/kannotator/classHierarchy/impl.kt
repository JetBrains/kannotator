package org.jetbrains.kannotator.classHierarchy

import java.util.ArrayList
import java.util.HashSet
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

data class ClassData(val name: ClassName, val methods: Collection<Method>)

private class ClassHierarchyEdgeImpl(
        override val parent: HierarchyNode<ClassData>,
        override val child: HierarchyNode<ClassData>): HierarchyEdge<ClassData>

private class ClassNodeImpl(val name: ClassName): HierarchyNode<ClassData> {
    override val children: MutableCollection<HierarchyEdge<ClassData>> = ArrayList()
    override val parents: MutableCollection<HierarchyEdge<ClassData>> = ArrayList()

    val methods: MutableSet<Method> = HashSet()

    override fun data(): ClassData = ClassData(name, methods)

    public fun toString(): String = name.internal
}

val HierarchyNode<ClassData>.methods: Collection<Method>
    get() = data().methods

val HierarchyNode<ClassData>.name: ClassName
    get() = data().name