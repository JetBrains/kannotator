package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.Nullability
import org.jetbrains.kannotator.mutability.MutabilityAnnotation

fun buildNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: Positions,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>
) : Annotations<NullabilityAnnotation> {
    val result = NullabilityAnnotationsInference(graph, annotations, positions, declarationIndex).buildAnnotations()
    return result as Annotations<NullabilityAnnotation>
}

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: Positions,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {
    val result = MutabilityAnnotationsInference(graph, annotations, positions, declarationIndex).buildAnnotations()
    return result as Annotations<MutabilityAnnotation>
}

