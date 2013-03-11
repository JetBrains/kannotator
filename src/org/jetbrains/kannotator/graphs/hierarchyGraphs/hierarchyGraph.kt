package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.graphs.*

trait HierarchyGraph<T>: Graph<T, Any?> {
    val hierarchyNodes: Collection<HierarchyNode<T>>
        get() = nodes as Collection<HierarchyNode<T>>
}

trait HierarchyNode<T>: Node<T, Any?>
trait HierarchyEdge<T>: Edge<T, Any?>

public val <T> HierarchyEdge<T>.parent: HierarchyNode<T>
        get() = this.from as HierarchyNode<T>

public val <T> HierarchyEdge<T>.child: HierarchyNode<T>
    get() = this.to as HierarchyNode<T>

public val <T> HierarchyNode<T>.parents: Collection<HierarchyEdge<T>>
    get() = this.incomingEdges as Collection<HierarchyEdge<T>>

public val <T> HierarchyNode<T>.children: Collection<HierarchyEdge<T>>
    get() = this.outgoingEdges as Collection<HierarchyEdge<T>>

public val <T> HierarchyNode<T>.parentNodes: Collection<HierarchyNode<T>>
    get() = this.predecessors as Collection<HierarchyNode<T>>

public val <T> HierarchyNode<T>.childNodes: Collection<HierarchyNode<T>>
    get() = this.successors as Collection<HierarchyNode<T>>

class HierarchyGraphImpl<T>(createNodeMap: Boolean): GraphImpl<T, Any?>(createNodeMap), HierarchyGraph<T>

abstract class HierarchyNodeImpl<T>: NodeImpl<T, Any?>(), HierarchyNode<T>

class HierarchyEdgeImpl<T>(
        from: HierarchyNodeImpl<T>, to: HierarchyNodeImpl<T>
): EdgeImpl<T, Any?>(null, from, to), HierarchyEdge<T>