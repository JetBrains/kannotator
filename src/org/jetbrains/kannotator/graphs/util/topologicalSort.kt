package org.jetbrains.kannotator.funDependecy

import funDependency.SCCFinder
import java.util.LinkedList
import java.util.HashSet
import kotlinlib.topologicallySort
import org.jetbrains.kannotator.graphs.Graph
import org.jetbrains.kannotator.graphs.Node

fun <A, L> Graph<A, L>.getTopologicallySortedStronglyConnectedComponents(): List<Set<Node<A, L>>> {
    val sccFinder = SCCFinder(this, { g -> g.nodes }, { m -> m.outgoingEdges.map{ it.to } })
    val components = sccFinder.getAllComponents()

    return components.topologicallySort {
        c ->
        c.map {
            m -> sccFinder.findComponent(m)
        }
    }
}