package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList

public trait FunDependencyGraph {
    val functions: Collection<FunctionNode>
}

public trait FunDependencyEdge {
    val from: FunctionNode
    val to: FunctionNode
}

public trait FunctionNode {
    val incomingEdges: Collection<FunDependencyEdge>
    val outgoingEdges: Collection<FunDependencyEdge>
    val name: String
}

fun addEdge(from: FunctionNodeImpl, to: FunctionNodeImpl) {
    val edge = FunDependencyEdgeImpl(from, to)

    from.outgoingEdges.add(edge)
    to.incomingEdges.add(edge)
}

class FunDependencyEdgeImpl(override val from: FunctionNode,
                            override val to: FunctionNode): FunDependencyEdge {
}

class FunctionNodeImpl(override val name: String): FunctionNode {
    override val outgoingEdges: MutableCollection<FunDependencyEdge> = ArrayList()
    override val incomingEdges: MutableCollection<FunDependencyEdge> = ArrayList()
}