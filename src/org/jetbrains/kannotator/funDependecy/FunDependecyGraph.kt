package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method

public trait FunDependencyGraph {
    val functions: List<FunctionNode>
}

public trait FunDependencyEdge {
    val from: FunctionNode
    val to: FunctionNode

    fun toString(): String {
        return "${from.method.asmMethod} -> ${to.method.asmMethod}"
    }
}

public trait FunctionNode {
    val incomingEdges: Collection<FunDependencyEdge>
    val outgoingEdges: Collection<FunDependencyEdge>
    val method: Method

    fun toString(): String {
        return "${method.asmMethod} in$incomingEdges out$outgoingEdges"
    }
}

fun addEdge(from: FunctionNodeImpl, to: FunctionNodeImpl) {
    val edge = FunDependencyEdgeImpl(from, to)

    from.outgoingEdges.add(edge)
    to.incomingEdges.add(edge)
}

class FunDependencyEdgeImpl(override val from: FunctionNode,
                            override val to: FunctionNode): FunDependencyEdge {
}

class FunctionNodeImpl(override val method: Method): FunctionNode {
    override val outgoingEdges: MutableCollection<FunDependencyEdge> = ArrayList()
    override val incomingEdges: MutableCollection<FunDependencyEdge> = ArrayList()
}