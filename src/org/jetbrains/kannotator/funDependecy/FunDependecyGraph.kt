package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method

public trait FunDependencyGraph {
    val functions: List<FunctionNode>
    val noOutgoingNodes: List<FunctionNode>
}

public trait FunDependencyEdge {
    val from: FunctionNode
    val to: FunctionNode
}

public trait FunctionNode {
    val incomingEdges: Collection<FunDependencyEdge>
    val outgoingEdges: Collection<FunDependencyEdge>
    val method: Method
}

class FunDependencyGraphImpl : FunDependencyGraph {
    override val noOutgoingNodes: List<FunctionNode> get() = noOutgoingNodesSet.toList()
    override val functions: List<FunctionNode> get() = nodes.values().toList()

    private val noOutgoingNodesSet = hashSet<FunctionNode>()
    private val nodes = hashMap<Method, FunctionNodeImpl>()

    fun getOrCreateNode(method : Method) : FunctionNodeImpl {
        return nodes.getOrPut(method, {
            val funNode = FunctionNodeImpl(method)
            noOutgoingNodesSet.add(funNode)
            funNode
        })
    }

    fun createEdge(from: FunctionNodeImpl, to: FunctionNodeImpl) : FunDependencyEdgeImpl {
        val edge = FunDependencyEdgeImpl(from, to)

        from.outgoingEdges.add(edge)
        to.incomingEdges.add(edge)

        noOutgoingNodesSet.remove(from)

        return edge
    }
}

class FunDependencyEdgeImpl(override val from: FunctionNode,
                            override val to: FunctionNode): FunDependencyEdge {

    fun toString(): String {
        return "${from.method} -> ${to.method}"
    }

    fun hashCode(): Int {
        return from.hashCode() * 31 + to.hashCode()
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is FunDependencyEdge) {
            return from == obj.from && to == obj.to
        }
        return false
    }
}

class FunctionNodeImpl(override val method: Method): FunctionNode {
    override val outgoingEdges: MutableCollection<FunDependencyEdge> = hashSet()
    override val incomingEdges: MutableCollection<FunDependencyEdge> = hashSet()

    fun toString(): String {
        return "${method} in$incomingEdges out$outgoingEdges"
    }
}