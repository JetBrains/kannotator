package org.jetbrains.kannotator.annotationsInference.propagation

import java.util.HashSet
import kotlinlib.*
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.Variance.*
import org.jetbrains.kannotator.declarations.getValidPositions
import java.util.LinkedHashSet
import org.jetbrains.kannotator.classHierarchy.parentNodes

fun propagateMetadata<A>(
        graph: HierarchyGraph<Method>,
        lattice: AnnotationLattice<A>,
        annotations: Annotations<A>
): Annotations<A> {
    val result = AnnotationsImpl(annotations)

    val leafMethods = graph.nodes.filter { it.children.isEmpty() }

    val allMethods = graph.nodes.map{ n -> n.method }.toSet()
    fun assertAllVisited(visitedMethods: Set<Method>) {
        val unvisited = allMethods - visitedMethods
        assert (unvisited.isEmpty()) { "Methods not visited: $unvisited" }
    }

    assertAllVisited(
            resolveAllAnnotationConflicts(leafMethods, lattice, result)
    )

    return result
}