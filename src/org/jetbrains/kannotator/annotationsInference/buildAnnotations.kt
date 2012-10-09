package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotationInferrer
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotationInferrer
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.index.DeclarationIndex

fun buildNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>
) : Annotations<NullabilityAnnotation> {
    return NullabilityAnnotationInferrer(graph, annotations, positions, declarationIndex).buildAnnotations()
}

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {
    return MutabilityAnnotationInferrer(graph, annotations, positions, declarationIndex).buildAnnotations()
}

