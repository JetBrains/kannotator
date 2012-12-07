package org.jetbrains.kannotator.funDependecy

import funDependency.SCCFinder
import java.util.LinkedList
import java.util.HashSet
import kotlinlib.topologicallySort

fun <A> DependencyGraph<A>.getTopologicallySortedStronglyConnectedComponents(): List<Set<DependencyNode<A>>> {
    val sccFinder = SCCFinder(this, { g -> g.allNodes }, { m -> m.outgoingEdges.map{ it.to } })
    val components = sccFinder.getAllComponents()

    return components.topologicallySort {
        c ->
        c.map {
            m -> sccFinder.findComponent(m)
        }
    }
}