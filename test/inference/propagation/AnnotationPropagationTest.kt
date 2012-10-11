package inference.propagation

import junit.framework.TestCase
import junit.framework.Assert.*
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation.*
import org.jetbrains.kannotator.declarations.Annotations
import kotlinlib.*
import util.getAllClassesWithPrefix
import util.ClassesFromClassPath
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.AnnotationPosition
import java.util.LinkedHashSet
import org.jetbrains.kannotator.annotations.io.toAnnotationKey

class AnnotationPropagationTest : TestCase() {

    fun doTest(classSource: ClassSource) {
        val classHierarchy = buildClassHierarchyGraph(classSource)
        val methodHierarchy = buildMethodHierarchy(classHierarchy)

        val initialAnnotations = getAnnotationsFromClassFiles(classSource) { names -> classNamesToNullabilityAnnotation(names) }

        val propagatedAnnotations = propagateMetadata(methodHierarchy, NullabiltyLattice, initialAnnotations)

        val EXPECT_NN = "inferenceData.annotations.ExpectNotNull"
        val EXPECT_N = "inferenceData.annotations.ExpectNullable"
        val NN = "org.jetbrains.annotations.NotNull"
        val N = "org.jetbrains.annotations.Nullable"
        val expectedAnnotationClasses = hashMap(
                NN to NOT_NULL,
                N to NULLABLE,
                EXPECT_NN to NOT_NULL,
                EXPECT_N to NULLABLE
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

    fun test() {
        doTest(ClassesFromClassPath(
                "inferenceData/propagation/Conflicts",
                "inferenceData/propagation/Child"
        ))
    }
}

fun Annotations<NullabilityAnnotation>.toDeclarations(): String {
    val positions = LinkedHashSet<AnnotationPosition>()
    forEach {
        pos, ann ->
        positions.add(pos)
    }
    val s = buildString {
        sb ->
        for (pos in positions.toSortedList { a, b -> a.toAnnotationKey().compareTo(b.toAnnotationKey()) }) {
            sb.append("${pos.toAnnotationKey()}\n  ${this[pos]}\n\n")
        }
    }.toSystemLineSeparators()
    return s
}

