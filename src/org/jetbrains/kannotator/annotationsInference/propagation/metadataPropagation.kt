package org.jetbrains.kannotator.annotationsInference.propagation

import java.util.HashSet
import kotlinlib.*
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import java.util.LinkedHashSet
import org.jetbrains.kannotator.classHierarchy.childNodes
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.getValidPositions
import org.jetbrains.kannotator.declarations.RETURN_TYPE
import org.jetbrains.kannotator.declarations.Variance

fun propagateMetadata<A>(
        graph: HierarchyGraph<Method>,
        lattice: AnnotationLattice<A>,
        annotations: Annotations<A>
): Annotations<A> {
    val result = AnnotationsImpl(annotations)

    val classifiedMethods = graph.nodes.classify {
        when {
            it.children.isEmpty() -> "leaf"
            it.parents.isEmpty() -> "root"
            else -> ""
        }
    }

    val leafMethods = classifiedMethods.getOrThrow("leaf", "No leaf methods. Can't be: no loops are possible")
    val rootMethods = classifiedMethods.getOrThrow("root", "No root methods. Can't be: no loops are possible")

    val allMethods = graph.nodes.map{ n -> n.method }.toSet()
    fun assertAllVisited(visitedMethods: Collection<Method>) {
        val unvisited = allMethods - visitedMethods
        assert (unvisited.isEmpty()) { "Methods not visited: $unvisited" }
    }

    assertAllVisited(
            resolveAllAnnotationConflicts(leafMethods, lattice, result)
    )

    assertAllVisited(
            propagateAllAnnotationsFromParentsToChildren(rootMethods, lattice, result)
    )

    return result
}

private fun propagateAllAnnotationsFromParentsToChildren<A>(
        rootMethodNodes: Collection<HierarchyNode<Method>>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>
): Collection<Method> {
    return bfs(rootMethodNodes) {
        node ->
        val childNodes = node.childNodes()

        propagateAnnotationsToImmediateChildren(
                node.method,
                childNodes.map{ node -> node.method },
                lattice,
                annotationsToFix
        )

        scheduleAll(childNodes)
    }
    .map { node -> node.method }
}

private fun propagateAnnotationsToImmediateChildren<A>(
        method: Method,
        immediateOverrides: Iterable<Method>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>
) {
    val typePositions = PositionsForMethod(method).getValidPositions()

    for (child in immediateOverrides) {
        val positionsWithinChild = PositionsForMethod(child)

        for (positionInParent in typePositions) {
            val relativePosition = positionInParent.relativePosition
            val positionInChild = positionsWithinChild[relativePosition].position

            val annotationInParent = annotationsToFix[positionInParent]
            val annotationInChild = annotationsToFix[positionInChild]

            if (annotationInParent == null) continue

            if (annotationInChild == null) {
                annotationsToFix[positionInChild] = annotationInParent
            }
            else {
                if (relativePosition == RETURN_TYPE) continue // Covariant return (conflicts have been resolved already)

                annotationsToFix[positionInChild] = lattice.unify<A>(
                        relativePosition, annotationInParent, annotationInChild)
            }
        }
    }
}