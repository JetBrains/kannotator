package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.mutability.MutabilityAnnotation

fun buildNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>
) : Annotations<NullabilityAnnotation> {
    return NullabilityAnnotationsInference(graph, annotations, positions, declarationIndex).buildAnnotations()
}

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {
    return MutabilityAnnotationsInference(graph, annotations, positions, declarationIndex).buildAnnotations()
}

