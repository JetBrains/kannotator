package org.jetbrains.kannotator.classHierarchy

import java.util.ArrayList
import java.util.HashSet
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

abstract class HierarchyNodeImpl<D> : HierarchyNode<D> {
    private val _children: MutableCollection<HierarchyEdge<D>> = ArrayList()
    private val _parents: MutableCollection<HierarchyEdge<D>> = ArrayList()

    override val children: Collection<HierarchyEdge<D>>
        get() = _children
    override val parents: Collection<HierarchyEdge<D>>
        get() = _parents

    fun addParent(edge: HierarchyEdge<D>) {
        _parents.add(edge)
    }

    fun addChild(edge: HierarchyEdge<D>) {
        _children.add(edge)
    }
}

class HierarchyEdgeImpl<D>(
        override val parent: HierarchyNode<D>,
        override val child: HierarchyNode<D>) : HierarchyEdge<D>