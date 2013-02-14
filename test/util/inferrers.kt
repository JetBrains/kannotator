package util

import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier

enum class InferrerKey {
    NULLABILITY
    MUTABILITY
}

public val INFERRERS: Map<InferrerKey, AnnotationInferrer<Any, Qualifier>> = hashMap(
        Pair(InferrerKey.NULLABILITY, NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>),
        Pair(InferrerKey.MUTABILITY, MUTABILITY_INFERRER as AnnotationInferrer<Any, Qualifier>)
)
