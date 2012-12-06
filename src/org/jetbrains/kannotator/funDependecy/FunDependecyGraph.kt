package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import java.util.LinkedHashSet
import java.util.LinkedHashMap

public trait FunDependencyGraph<A> {
    val functions: List<FunctionNode<A>>
    val noOutgoingNodes: List<FunctionNode<A>>
}

public trait FunDependencyEdge<A> {
    val from: FunctionNode<A>
    val to: FunctionNode<A>
}

public trait FunctionNode<A> {
    val incomingEdges: Collection<FunDependencyEdge<A>>
    val outgoingEdges: Collection<FunDependencyEdge<A>>
    val data: A
}

class FunDependencyGraphImpl<A> : FunDependencyGraph<A> {
    override val noOutgoingNodes: List<FunctionNode<A>> get() = noOutgoingNodesSet.toList()
    override val functions: List<FunctionNode<A>> get() = nodes.values().toList()

    private val noOutgoingNodesSet = LinkedHashSet<FunctionNode<A>>()
    private val nodes = LinkedHashMap<A, FunctionNodeImpl<A>>()

    fun getOrCreateNode(method : A) : FunctionNodeImpl<A> {
        return nodes.getOrPut(method, {
            val funNode = FunctionNodeImpl<A>(method)
            noOutgoingNodesSet.add(funNode)
            funNode
        })
    }

    fun createEdge(from: FunctionNodeImpl<A>, to: FunctionNodeImpl<A>, debugName: String? = null) : FunDependencyEdgeImpl<A> {
        val edge = FunDependencyEdgeImpl(from, to, debugName)

        from.outgoingEdges.add(edge)
        to.incomingEdges.add(edge)

        noOutgoingNodesSet.remove(from)

        return edge
    }
}

class FunDependencyEdgeImpl<A>(override val from: FunctionNode<A>,
                            override val to: FunctionNode<A>,
                            val debugName: String? = null): FunDependencyEdge<A> {

    fun toString(): String {
        val prefix = if (debugName != null) debugName + " " else ""
        return "${prefix}${from.data} -> ${to.data}"
    }

    fun hashCode(): Int {
        return from.hashCode() * 31 + to.hashCode()
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is FunDependencyEdge<*>) {
            return from == obj.from && to == obj.to
        }
        return false
    }
}

class FunctionNodeImpl<A>(override val data: A): FunctionNode<A> {
    override val outgoingEdges: MutableCollection<FunDependencyEdge<A>> = LinkedHashSet()
    override val incomingEdges: MutableCollection<FunDependencyEdge<A>> = LinkedHashSet()

    fun toString(): String {
        return "${data} in$incomingEdges out$outgoingEdges"
    }
}