package org.jetbrains.kannotator.annotationsInference.propagation

import kotlinlib.*
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.getValidPositions
import org.jetbrains.kannotator.declarations.getSignatureDescriptor
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.ParameterPosition
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.MethodTypePosition
import java.util.ArrayList

public val JB_PROPAGATED: String = "org.jetbrains.kannotator.runtime.annotations.Propagated"
public val JB_PROPAGATION_KIND: String = "org.jetbrains.kannotator.runtime.annotations.PropagationKind"

fun propagateMetadata<A>(
        graph: HierarchyGraph<Method>,
        lattice: AnnotationLattice<A>,
        annotations: Annotations<A>,
        propagatedPositionsToFill: MutableSet<AnnotationPosition>,
        propagationOverrides: Annotations<A>
): Annotations<A> {
    val result = AnnotationsImpl(annotations)

    val classifiedMethods = graph.hierarchyNodes.groupBy { node ->
        when {
            node.children.isEmpty() -> "leaf"
            node.parents.isEmpty() -> "root"
            else -> ""
        }
    }

    val leafMethods = classifiedMethods["leaf"]
    if (leafMethods == null) {
        return result
    }

    val allMethods = graph.nodes.map{ n -> n.method }.toSet()
    fun assertAllVisited(visitedMethods: Collection<Method>) {
        val unvisited = allMethods.subtract(visitedMethods)
        assert (unvisited.isEmpty()) { "Methods not visited: $unvisited" }
    }

    assertAllVisited(
            resolveAllAnnotationConflicts(leafMethods, lattice, result, propagatedPositionsToFill)
    )

    propagateParameterAnnotations(allMethods, lattice, result, propagatedPositionsToFill)
    propagateOverrides(graph, propagationOverrides, result, propagatedPositionsToFill)

    return result
}

fun updateAnnotations<A>(
        annotationsToFix: MutableAnnotations<A>,
        pos: AnnotationPosition,
        newAnnotation: A,
        propagatedPositionsToFill: MutableSet<AnnotationPosition>) {
    if (annotationsToFix[pos] != newAnnotation) {
        annotationsToFix[pos] = newAnnotation
        propagatedPositionsToFill.add(pos)
    }
}

private fun propagateOverrides<A>(
        graph: HierarchyGraph<Method>,
        propagationOverrides: Annotations<A>,
        annotationsToFix: MutableAnnotations<A>,
        propagatedPositionsToFill: MutableSet<AnnotationPosition>
) {
    propagationOverrides forEach {(pos, ann) ->
        if (pos is MethodTypePosition) {
            val methodNode = graph.findNode(pos.method)
            if (methodNode != null) {
                bfs(listOf(methodNode)) {
                    val node = it as HierarchyNode<Method>
                    val method = node.data
                    val currentPos = PositionsForMethod(method)[pos.relativePosition].position
                    updateAnnotations(annotationsToFix, currentPos, ann, propagatedPositionsToFill)

                    node.childNodes
                }
            }
        }
    }
}

private fun propagateParameterAnnotations<A>(
        methods: Collection<Method>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>,
        propagatedPositionsToFill: MutableSet<AnnotationPosition>
) {
    val methodsBySignature = methods.groupBy {method -> method.id.getSignatureDescriptor()}
    for ((_, groupedMethods) in methodsBySignature) {
        assert (!groupedMethods.isEmpty()) {"groupedMethods is empty for $_"}
        val positionsForMethods = groupedMethods.map {method -> PositionsForMethod(method)}
        for (position in positionsForMethods.first().getValidPositions()) {
            val declPos = position.relativePosition
            if (declPos !is ParameterPosition)
                continue
            val annotations = ArrayList<A>()
            for (positionsForMethod in positionsForMethods) {
                val annotatedType = positionsForMethod[declPos]
                val annotationPosition = annotatedType.position
                val annotationValue = annotationsToFix[annotationPosition]
                if (annotationValue != null) {
                    annotations.add(annotationValue)
                }
            }

            //val annotations = positionsForMethods.map{ annotationsToFix[it[declPos].position] }.filterNotNull()

            if (!annotations.isEmpty()) {
                val unifiedAnnotation = lattice.unify<A>(declPos, annotations)
                for (positions in positionsForMethods) {
                    val pos = positions[declPos].position
                    updateAnnotations(annotationsToFix, pos, unifiedAnnotation, propagatedPositionsToFill)
                }
            }
        }
    }
}