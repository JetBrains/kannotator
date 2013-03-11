package org.jetbrains.kannotator.graphs

import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.union

open class GraphImpl<out T, out L>(private createNodeMap: Boolean): Graph<T, L> {
    private val _nodes: MutableCollection<Node<T, L>> = LinkedHashSet()
    private val nodeMap: MutableMap<T, Node<T, L>>? = if (createNodeMap) HashMap<T, Node<T, L>>() else null

    override val nodes: Collection<Node<T, L>> = _nodes

    override fun findNode(data: T): Node<T, L>? = nodeMap?.get(data)

    fun addNode(node: Node<T, L>) {
        _nodes.add(node)
        nodeMap?.put(node.data, node)
    }

    fun removeNode(node: Node<T, L>) {
        _nodes.remove(node)
        nodeMap?.remove(node.data)
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

    fun removeIncomingEdge(edge: Edge<T, L>) {
        _incomingEdges.remove(edge)
    }

    fun removeOutgoingEdge(edge: Edge<T, L>) {
        _outgoingEdges.remove(edge)
    }

    open fun toString(): String {
        return "${data} in$incomingEdges out$outgoingEdges"
    }
}

class DefaultNodeImpl<out T, out L>(public override val data: T) : NodeImpl<T, L>()

open class EdgeImpl<T, L>(
        public override val label: L,
        public override val from: NodeImpl<T, L>,
        public override val to: NodeImpl<T, L>
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

    fun removeNode(n: NodeImpl<NodeData, EdgeLabel>) {
        val edges = n.incomingEdges.union(n.outgoingEdges)
        for (e in edges) {
            removeEdge(e as EdgeImpl<NodeData, EdgeLabel>)
        }
        graph.removeNode(n)
    }

    fun removeEdge(e: EdgeImpl<NodeData, EdgeLabel>) {
        e.from.removeOutgoingEdge(e)
        e.to.removeIncomingEdge(e)
    }

    fun toGraph(): G = graph
}

fun <NodeKey, NodeData, EdgeLabel, G: GraphImpl<NodeData, EdgeLabel>> GraphBuilder<NodeKey, NodeData, EdgeLabel, G>.restrictGraphNodes(
        nodeIsRestricted: (Node<NodeData, EdgeLabel>) -> Boolean
) {
    val restrictedNodes = graph.nodes.filter(nodeIsRestricted)
    for (node in restrictedNodes) {
        this.removeNode(node as NodeImpl<NodeData, EdgeLabel>)
    }
}