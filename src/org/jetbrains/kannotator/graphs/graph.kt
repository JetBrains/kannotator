package org.jetbrains.kannotator.graphs

public trait Graph<out T, out L> {
    public val nodes: Collection<Node<T, L>>
    public fun findNode(data: T): Node<T, L>?
}

public trait Edge<out T, out L> {
    public val label: L
    public val from: Node<T, L>
    public val to: Node<T, L>
}

public trait Node<out T, out L> {
    public val data: T
    public val incomingEdges: Collection<Edge<T, L>>
    public val outgoingEdges: Collection<Edge<T, L>>
}

public val <T, L> Node<T, L>.predecessors: Collection<Node<T, L>>
    get() = incomingEdges.map { e -> e.from }
public val <T, L> Node<T, L>.successors: Collection<Node<T, L>>
    get() = outgoingEdges.map { e -> e.to }

public trait GraphBuilder<in TI, out TO, out L, N: Node<TO, L>, E: Edge<TO, L>> {
    public fun getOrCreateNode(data: TI): N
    public fun getOrCreateEdge(label: L, from: N, to: N): E

    public fun toGraph(): Graph<TO, L>
}
