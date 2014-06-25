package inference.propagation

import junit.framework.Assert.*
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation.*
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.ClassSource
import java.util.LinkedHashSet
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey

fun doPropagationTest(
        classSource: ClassSource,
        propagate: (HierarchyGraph<Method>, Annotations<NullabilityAnnotation>) -> Annotations<NullabilityAnnotation>
) {
    val classHierarchy = buildClassHierarchyGraph(classSource)
    val methodHierarchy = buildMethodHierarchy(classHierarchy)

    val initialAnnotations = getAnnotationsFromClassFiles(classSource) { names -> classNamesToNullabilityAnnotation(names) }

    val propagatedAnnotations = propagate(methodHierarchy, initialAnnotations)

    val EXPECT_NN = "inferenceData.annotations.ExpectNotNull"
    val EXPECT_N = "inferenceData.annotations.ExpectNullable"
    val NN = "org.jetbrains.annotations.NotNull"
    val N = "org.jetbrains.annotations.Nullable"
    val expectedAnnotationClasses = hashMapOf(
            NN to NOT_NULL,
            N to NULLABLE
    )
    val expectedAnnotations = getAnnotationsFromClassFiles(classSource) {
        names ->
        when {
            names.isEmpty() -> null
            EXPECT_N in names -> NULLABLE
            EXPECT_NN in names -> NOT_NULL
            else -> {
                assert (names.size == 1) {"Multiple annotations but no Expect* ones: $names"}
                expectedAnnotationClasses[names.first()]
            }
        }
    }

    assertEquals(expectedAnnotations.toDeclarations(), propagatedAnnotations.toDeclarations())
}


fun Annotations<NullabilityAnnotation>.toDeclarations(): String {
    val positions = LinkedHashSet<AnnotationPosition>()
    forEach {
        pos, ann ->
        positions.add(pos)
    }
    val s = StringBuilder {
        for (pos in positions.toSortedList { a, b -> a.toAnnotationKey().compareTo(b.toAnnotationKey()) }) {
            append("${pos.toAnnotationKey()}\n  ${this@toDeclarations[pos]}\n\n")
        }
    }.toString().toSystemLineSeparators()
    return s
}