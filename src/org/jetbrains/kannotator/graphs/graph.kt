package org.jetbrains.kannotator.graphs

public interface Graph<T, out L> {
    public val nodes: Collection<Node<T, L>>
    public fun findNode(data: T): Node<T, L>?
}

public interface Edge<T, out L> {
    public val label: L
    public val from: Node<T, L>
    public val to: Node<T, L>
}

public interface Node<T, out L> {
    public val data: T
    public val incomingEdges: Collection<Edge<T, L>>
    public val outgoingEdges: Collection<Edge<T, L>>
}

public val <T, L> Node<T, L>.predecessors: Collection<Node<T, L>>
    get() = incomingEdges.map { e -> e.from }
public val <T, L> Node<T, L>.successors: Collection<Node<T, L>>
    get() = outgoingEdges.map { e -> e.to }