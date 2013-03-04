package org.jetbrains.kannotator.graphs

import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashSet

open class GraphImpl<out T, out L>(private createNodeMap: Boolean): Graph<T, L> {
    private val _nodes: MutableCollection<Node<T, L>> = ArrayList()
    private val nodeMap: MutableMap<T, Node<T, L>>? = if (createNodeMap) HashMap<T, Node<T, L>>() else null

    override val nodes: Collection<Node<T, L>> = _nodes

    override fun findNode(data: T): Node<T, L>? = nodeMap?.get(data)

    fun addNode(node: Node<T, L>) {
        _nodes.add(node)
        nodeMap?.put(node.data, node)
    }
}

abstract class NodeImpl<out T, out L> : Node<T, L> {
    private val _incomingEdges: MutableCollection<Edge<T, L>> = LinkedHashSet()
    private val _outgoingEdges: MutableCollection<Edge<T, L>> = LinkedHashSet()

    override val incomingEdges: Collection<Edge<T, L>> = _incomingEdges
    override val outgoingEdges: Collection<Edge<T, L>> = _outgoingEdges

    fun addIncomingEdge(edge: Edge<T, L>) {
        _incomingEdges.add(edge)
    }

    fun addOutgoingEdge(edge: Edge<T, L>) {
        _outgoingEdges.add(edge)
    }

    open fun toString(): String {
        return "${data} in$incomingEdges out$outgoingEdges"
    }
}

class DefaultNodeImpl<out T, out L>(public override val data: T) : NodeImpl<T, L>()

open class EdgeImpl<T, L>(
        public override val label: L,
        public override val from: Node<T, L>,
        public override val to: Node<T, L>
) : Edge<T, L> {
    fun toString(): String {
        val prefix = if (label != null) "$label " else ""
        return "${prefix}${from.data} -> ${to.data}"
    }

    fun hashCode(): Int {
        return from.hashCode() * 31 + to.hashCode()
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is EdgeImpl<*, *>) {
            return from == obj.from && to == obj.to
        }
        return false
    }
}

abstract class GraphBuilder<NodeKey, NodeData, EdgeLabel, G: GraphImpl<NodeData, EdgeLabel>>(
        val createNodeMap: Boolean, cacheNodes: Boolean
) {
    val nodeCache = if (cacheNodes) HashMap<NodeKey, NodeImpl<NodeData, EdgeLabel>>() else null

    val graph: G = newGraph()

    abstract fun newGraph(): G
    abstract fun newNode(data: NodeKey): NodeImpl<NodeData, EdgeLabel>
    open fun newEdge(label: EdgeLabel, from: NodeImpl<NodeData, EdgeLabel>, to: NodeImpl<NodeData, EdgeLabel>): EdgeImpl<NodeData, EdgeLabel> = EdgeImpl(label, from, to)

    fun getOrCreateNode(data: NodeKey): NodeImpl<NodeData, EdgeLabel> {
        val cachedNode = nodeCache?.get(data)
        if (cachedNode != null) {
            return cachedNode
        }

        val node = newNode(data)
        graph.addNode(node)
        nodeCache?.put(data, node)
        return node
    }

    fun getOrCreateEdge(label: EdgeLabel, from: NodeImpl<NodeData, EdgeLabel>, to: NodeImpl<NodeData, EdgeLabel>): EdgeImpl<NodeData, EdgeLabel> {
        val edge = newEdge(label, from, to)
        from.addOutgoingEdge(edge)
        to.addIncomingEdge(edge)
        return edge
    }

    fun toGraph(): G = graph
}