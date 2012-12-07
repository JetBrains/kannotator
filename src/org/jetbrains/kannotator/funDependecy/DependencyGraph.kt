package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import java.util.LinkedHashSet
import java.util.LinkedHashMap

public trait DependencyGraph<A> {
    val allNodes: List<DependencyNode<A>>
    val noOutgoingNodes: List<DependencyNode<A>>
}

public trait DependencyEdge<A> {
    val from: DependencyNode<A>
    val to: DependencyNode<A>
}

public trait DependencyNode<A> {
    val incomingEdges: Collection<DependencyEdge<A>>
    val outgoingEdges: Collection<DependencyEdge<A>>
    val data: A
}

class DependencyGraphImpl<A> : DependencyGraph<A> {
    override val noOutgoingNodes: List<DependencyNode<A>> get() = noOutgoingNodesSet.toList()
    override val allNodes: List<DependencyNode<A>> get() = nodes.values().toList()

    private val noOutgoingNodesSet = LinkedHashSet<DependencyNode<A>>()
    private val nodes = LinkedHashMap<A, DependencyNodeImpl<A>>()

    fun getOrCreateNode(method : A) : DependencyNodeImpl<A> {
        return nodes.getOrPut(method, {
            val funNode = DependencyNodeImpl<A>(method)
            noOutgoingNodesSet.add(funNode)
            funNode
        })
    }

    fun createEdge(from: DependencyNodeImpl<A>, to: DependencyNodeImpl<A>, debugName: String? = null) : DependencyEdgeImpl<A> {
        val edge = DependencyEdgeImpl(from, to, debugName)

        from.outgoingEdges.add(edge)
        to.incomingEdges.add(edge)

        noOutgoingNodesSet.remove(from)

        return edge
    }
}

class DependencyEdgeImpl<A>(override val from: DependencyNode<A>,
                            override val to: DependencyNode<A>,
                            val debugName: String? = null): DependencyEdge<A> {

    fun toString(): String {
        val prefix = if (debugName != null) debugName + " " else ""
        return "${prefix}${from.data} -> ${to.data}"
    }

    fun hashCode(): Int {
        return from.hashCode() * 31 + to.hashCode()
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is DependencyEdge<*>) {
            return from == obj.from && to == obj.to
        }
        return false
    }
}

class DependencyNodeImpl<A>(override val data: A): DependencyNode<A> {
    override val outgoingEdges: MutableCollection<DependencyEdge<A>> = LinkedHashSet()
    override val incomingEdges: MutableCollection<DependencyEdge<A>> = LinkedHashSet()

    fun toString(): String {
        return "${data} in$incomingEdges out$outgoingEdges"
    }
}