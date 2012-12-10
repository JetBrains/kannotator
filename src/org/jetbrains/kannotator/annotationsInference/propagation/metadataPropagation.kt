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
import org.jetbrains.kannotator.declarations.getSignatureDescriptor
import org.jetbrains.kannotator.declarations.AnnotationPosition
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.ParameterPosition
import org.jetbrains.kannotator.classHierarchy.parentNodes
import org.jetbrains.kannotator.declarations.MethodTypePosition

fun propagateMetadata<A>(
        graph: HierarchyGraph<Method>,
        lattice: AnnotationLattice<A>,
        annotations: Annotations<A>,
        propagationOverrides: Annotations<A> = AnnotationsImpl<A>()
): Annotations<A> {
    val result = AnnotationsImpl(annotations)

    val classifiedMethods = graph.nodes.classify {
        when {
            it.children.isEmpty() -> "leaf"
            it.parents.isEmpty() -> "root"
            else -> ""
        }
    }

    val leafMethods = classifiedMethods["leaf"]
    if (leafMethods == null) {
        return result
    }

    val allMethods = graph.nodes.map{ n -> n.method }.toSet()
    fun assertAllVisited(visitedMethods: Collection<Method>) {
        val unvisited = allMethods - visitedMethods
        assert (unvisited.isEmpty()) { "Methods not visited: $unvisited" }
    }

    assertAllVisited(
            resolveAllAnnotationConflicts(leafMethods, lattice, result)
    )

    propagateParameterAnnotations(allMethods, lattice, result)
    propagateOverrides(graph, propagationOverrides, result)

    return result
}

private fun propagateOverrides<A>(
        graph: HierarchyGraph<Method>,
        propagationOverrides: Annotations<A>,
        annotationsToFix: MutableAnnotations<A>
) {
    propagationOverrides forEach {(pos, ann) ->
        if (pos is MethodTypePosition) {
            val methodNode = graph.findNode(pos.method)
            if (methodNode != null) {
                bfs(arrayList(methodNode)) {node ->
                    val method = node!!.data
                    val currentPos = PositionsForMethod(method)[pos.relativePosition].position
                    annotationsToFix[currentPos] = ann
                    scheduleAll(node.childNodes())
                }
            }
        }
    }
}

private fun propagateParameterAnnotations<A>(
        methods: Collection<Method>,
        lattice: AnnotationLattice<A>,
        annotationsToFix: MutableAnnotations<A>
) {
    val methodsBySignature = methods.classify {method -> method.id.getSignatureDescriptor()}
    for ((_, groupedMethods) in methodsBySignature) {
        assert (!groupedMethods.isEmpty()) {"groupedMethods is empty for $_"}
        val positionsForMethods = groupedMethods.map {method -> PositionsForMethod(method)}
        for (position in positionsForMethods.first().getValidPositions()) {
            val declPos = position.relativePosition
            if (declPos !is ParameterPosition)
                continue
            val annotations = arrayList<A>()
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
                    annotationsToFix[pos] = unifiedAnnotation
                }
            }
        }
    }
}