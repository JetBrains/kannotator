package util

import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.controlFlow.builder.analysis.AnalysisType
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY

public val INFERRERS: Map<AnalysisType, AnnotationInferrer<Any, Qualifier>> = hashMap(
        Pair(NULLABILITY_KEY, NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>),
        Pair(MUTABILITY_KEY, MUTABILITY_INFERRER as AnnotationInferrer<Any, Qualifier>)
)
