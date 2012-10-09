package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

trait HierarchyGraph<D> {
    val nodes: Collection<HierarchyNode<D>>
}

trait HierarchyEdge<D> {
    val parent: HierarchyNode<D>
    val child: HierarchyNode<D>
}

trait HierarchyNode<D> {
    val children: Collection<HierarchyEdge<D>>
    val parents: Collection<HierarchyEdge<D>>

    val data: D
}

public fun <D> HierarchyNode<D>.parentNodes(): Collection<HierarchyNode<D>> = parents.map { e -> e.parent }
public fun <D> HierarchyNode<D>.childNodes(): Collection<HierarchyNode<D>> = children.map { e -> e.parent }