package org.jetbrains.kannotator.main

import java.util.HashMap
import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.annotationsInference.propagation.*
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.controlFlow.builder.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.annotationsInference.engine.*

class NullabilityInferrer: AnnotationInferrer<NullabilityAnnotation, Nullability> {
    private val methodToFieldNullabilityInfo = HashMap<Method, Map<Field, Nullability>>()

    override fun resolveAnnotation(classNames: Set<String>) = classNamesToNullabilityAnnotation(classNames)

    override fun inferAnnotationsFromFieldValue(field: Field): Annotations<NullabilityAnnotation> {
        val result = AnnotationsImpl<NullabilityAnnotation>()
        result.setIfNotNull(getFieldTypePosition(field), inferNullabilityFromFieldValue(field: Field))
        return result
    }

    override fun <Q: Qualifier> inferAnnotationsFromMethod(
            method: Method,
            methodNode: MethodNode,
            analysisResult: AnalysisResult<QualifiedValueSet<Q>>,
            fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<NullabilityAnnotation>): Annotations<NullabilityAnnotation> {
        val inferResult = buildMethodNullabilityAnnotations(
                method,
                methodNode,
                analysisResult,
                fieldDependencyInfoProvider,
                declarationIndex,
                annotations,
                { m -> methodToFieldNullabilityInfo[m] })

        methodToFieldNullabilityInfo[method] = inferResult.writtenFieldValueInfos

        return inferResult.inferredAnnotations
    }

    override val lattice: AnnotationLattice<NullabilityAnnotation> = NullabiltyLattice

    override val qualifierSet: QualifierSet<Nullability> = NullabilitySet

    override fun getFrameTransformer(
            annotations: Annotations<NullabilityAnnotation>,
            declarationIndex: DeclarationIndex): FrameTransformer<QualifiedValueSet<*>> {
        return NullabilityFrameTransformer(annotations, declarationIndex)
    }

    override fun getQualifierEvaluator(
            positions: PositionsForMethod,
            annotations: Annotations<NullabilityAnnotation>,
            declarationIndex: DeclarationIndex
    ): QualifierEvaluator<Nullability> {
        return NullabilityQualifierEvaluator(positions, annotations, declarationIndex)
    }
}

public val MUTABILITY_INFERRER_OBJECT: AnnotationInferrer<MutabilityAnnotation, Mutability> = MUTABILITY_INFERRER

object MUTABILITY_INFERRER: AnnotationInferrer<MutabilityAnnotation, Mutability> {
    override fun resolveAnnotation(classNames: Set<String>) =
            classNamesToMutabilityAnnotation(classNames)

    override fun inferAnnotationsFromFieldValue(field: Field): Annotations<MutabilityAnnotation> =
            AnnotationsImpl<MutabilityAnnotation>()

    override fun <Q: Qualifier> inferAnnotationsFromMethod(
            method: Method,
            methodNode: MethodNode,
            analysisResult: AnalysisResult<QualifiedValueSet<Q>>,
            fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<MutabilityAnnotation>
    ): Annotations<MutabilityAnnotation> {
        return buildMutabilityAnnotations(method, analysisResult)
    }

    override val lattice: AnnotationLattice<MutabilityAnnotation> = MutabiltyLattice

    override val qualifierSet: QualifierSet<Mutability> = MutabilitySet

    override fun getFrameTransformer(
            annotations: Annotations<MutabilityAnnotation>, declarationIndex: DeclarationIndex
    ): FrameTransformer<QualifiedValueSet<*>> {
        return MutabilityFrameTransformer(annotations, declarationIndex)
    }

    override fun getQualifierEvaluator(
            positions: PositionsForMethod,
            annotations: Annotations<MutabilityAnnotation>,
            declarationIndex: DeclarationIndex
    ): QualifierEvaluator<Mutability> {
        return MutabilityQualifierEvaluator
    }
}