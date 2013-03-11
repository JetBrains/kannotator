package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import org.jetbrains.kannotator.graphs.*
import java.util.HashSet
import kotlinlib.subtract
import kotlinlib.bfs

val <A, L> Graph<A, L>.sourceNodes: Collection<Node<A, L>>
    get() = nodes.filter { it.incomingEdges.empty }
val <A, L> Graph<A, L>.sinkNodes: Collection<Node<A, L>>
    get() = nodes.filter { it.outgoingEdges.empty }

fun <A, L> Graph<A, L>.extractNonAffectingNodes(nodeIsInteresting: (Node<A, L>) -> Boolean): Set<Node<A, L>> {
    val interestingNodes = this.nodes.filter {nodeIsInteresting(it)}
    val affectingNodes = HashSet(interestingNodes)

    bfs(interestingNodes) {node ->
        affectingNodes.add(node)
        scheduleAll(node.successors)
    }

    return this.nodes.subtract(affectingNodes)
}