package util

import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER

enum class InferrerKey {
    NULLABILITY
    MUTABILITY
}

public val INFERRERS: Map<InferrerKey, AnnotationInferrer<Any>> = hashMap(
        Pair(InferrerKey.NULLABILITY, NullabilityInferrer() as AnnotationInferrer<Any>),
        Pair(InferrerKey.MUTABILITY, MUTABILITY_INFERRER as AnnotationInferrer<Any>)
)
