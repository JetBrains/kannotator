package org.jetbrains.kannotator.annotationsInference.propagation

import java.util.HashSet
import kotlinlib.*
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
import org.jetbrains.kannotator.declarations.RETURN_TYPE
import org.jetbrains.kannotator.graphs.Node
import org.jetbrains.kannotator.classHierarchy.HierarchyNode

fun resolveAllAnnotationConflicts<A>(
        leafMethodNodes: Collection<HierarchyNode<Method>>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>
): Set<Method> {
    val visited = HashSet<Method>()
    for (leafMethodNode in leafMethodNodes) {
        visited.addAll(resolveAnnotationConflicts(leafMethodNode, lattice, annotationsToFix))
    }
    return visited
}

private fun resolveAnnotationConflicts<A>(
        leafMethod: HierarchyNode<Method>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>
): Collection<Method> {
    val propagatedAnnotations = AnnotationsImpl(annotationsToFix)

    return bfs(arrayList(leafMethod)) {
        node ->
        val parentNodes = node.parentNodes

        resolveConflictsInParents(
                node.method,
                parentNodes.map {node -> node.method},
                lattice,
                annotationsToFix,
                propagatedAnnotations)

        parentNodes
    }.map { node -> node.method }
}

private fun resolveConflictsInParents<A>(
        method: Method,
        immediateOverridden: Collection<Method>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>,
        propagatedAnnotations: MutableAnnotations<A>
) {
    val typePositions = PositionsForMethod(method).getValidPositions()

    for (parent in immediateOverridden) {
        val positionsWithinParent = PositionsForMethod(parent)

        for (positionInChild in typePositions) {
            val relativePosition = positionInChild.relativePosition
            val positionInParent = positionsWithinParent[relativePosition].position

            val fromChild = propagatedAnnotations[positionInChild]
            val inParent = annotationsToFix[positionInParent]

            if (fromChild != null) {
                if (inParent != null) {
                    annotationsToFix[positionInParent] = lattice.unify<A>(
                            relativePosition, inParent, fromChild)
                }
                else {
                    propagatedAnnotations[positionInParent] = fromChild
                    // propagate return value annotation up the graph
                    if (relativePosition == RETURN_TYPE) {
                        annotationsToFix[positionInParent] = fromChild
                    }
                }
            }
        }
    }
}

private val Node<Method, *>.method: Method
    get() = data