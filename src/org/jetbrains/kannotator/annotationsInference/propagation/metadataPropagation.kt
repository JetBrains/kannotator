package org.jetbrains.kannotator.annotationsInference.propagation

import java.util.HashSet
import java.util.LinkedList
import kotlinlib.*
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import org.jetbrains.kannotator.classHierarchy.parentNodes
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.Variance.*
import org.jetbrains.kannotator.declarations.getValidPositions
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration
import org.jetbrains.kannotator.declarations.setIfNotNull

fun propagateMetadata<A>(
        graph: HierarchyGraph<Method>,
        lattice: AnnotationLattice<A>,
        annotations: Annotations<A>
): Annotations<A> {
    val result = AnnotationsImpl(annotations)

    val leafMethods = graph.nodes.filter { it.children.isEmpty() }
    val conflictsResolvedFor = resolveAnnotationConflicts(leafMethods, lattice, result)

    val unvisited = graph.nodes.map{ n -> n.method }.toSet() - conflictsResolvedFor
    assert (unvisited.isEmpty()) { "Methods not visited: $unvisited" }

    return result
}

private fun resolveAnnotationConflicts<A>(
        leafMethods: List<HierarchyNode<Method>>,
        lattice: AnnotationLattice<A>,
        annotations: MutableAnnotations<A>
): Set<Method> {
    val visited = HashSet<Method>()

    val queue = LinkedList(leafMethods)
    while (!queue.isEmpty()) {
        val node = queue.pop()
        val method = node.method
        visited.add(method)

        val typePositions = PositionsForMethod(method).getValidPositions()

        val parentNodes = node.parentNodes()
        val propagatedAnnotations = AnnotationsImpl(annotations)

        for (parent in parentNodes) {
            val positionsWithinParent = PositionsForMethod(parent.method)

            for (positionInChild in typePositions) {
                val positionWithinMethod = positionInChild.relativePosition
                val positionInParent = positionsWithinParent[positionWithinMethod].position

                val fromChild = propagatedAnnotations[positionInChild]
                val inParent = annotations[positionInParent]

                if (fromChild != null) {
                    if (inParent != null) {
                        annotations[positionInParent] = lattice.resolveConflictInParent<A>(
                                positionWithinMethod, inParent, fromChild)
                    }
                    propagatedAnnotations.setIfNotNull(positionInParent, annotations[positionInParent])
                }
            }
        }

    }

    return visited
}

fun <A> AnnotationLattice<A>.resolveConflictInParent(position: PositionWithinDeclaration, parent: A, child: A): A {
    return when (position.variance) {
        COVARIANT -> leastCommonUpperBound(parent, child)
        CONTRAVARIANT -> greatestCommonLowerBound(parent, child)
        INVARIANT -> throw UnsupportedOperationException("Invariant position is not supported: $position")
    }
}

private val HierarchyNode<Method>.method: Method
    get() = data