package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import org.jetbrains.kannotator.graphs.Graph
import org.jetbrains.kannotator.graphs.Edge
import org.jetbrains.kannotator.graphs.Node
import org.jetbrains.kannotator.graphs.GraphImpl

public trait DependencyGraph<A, L>: Graph<A, L> {
    val noOutgoingNodes: Collection<Node<A, L>>
}

class DependencyGraphImpl<A, L>(createNodeMap: Boolean) : GraphImpl<A, L>(createNodeMap), DependencyGraph<A, L> {
    var _noOutgoingNodes: Collection<Node<A, L>>? = null

    override val noOutgoingNodes: Collection<Node<A, L>>
        get() {
            if (_noOutgoingNodes == null) {
                _noOutgoingNodes = nodes.filter { it.outgoingEdges.empty }
            }
            return _noOutgoingNodes!!
        }
}