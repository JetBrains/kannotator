package util

import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER

public val INFERRERS: Map<String, AnnotationInferrer<Any>> = hashMap(
        Pair("nullability", NullabilityInferrer() as AnnotationInferrer<Any>),
        Pair("mutability", MUTABILITY_INFERRER as AnnotationInferrer<Any>)
)
