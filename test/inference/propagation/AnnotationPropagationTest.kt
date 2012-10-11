package inference.propagation

import inferenceData.propagation.Conflicts
import inferenceData.propagation.DiamondHierarchy
import inferenceData.propagation.LinearHierarchy
import inferenceData.propagation.YHierarchy
import java.util.LinkedHashSet
import junit.framework.Assert.*
import junit.framework.TestCase
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.ClassSource
import util.Classes
import inferenceData.propagation.*

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

    fun testConflicts() {
        doTest(Classes(
                javaClass<Conflicts.Base>(),
                javaClass<Conflicts.Child>()
        ))
    }

    fun testLinearHierarchy() {
        doTest(Classes(
                javaClass<LinearHierarchy.A>(),
                javaClass<LinearHierarchy.B>(),
                javaClass<LinearHierarchy.C>()
        ))
    }

    fun testYHierarchy() {
        doTest(Classes(
                javaClass<YHierarchy.A>(),
                javaClass<YHierarchy.A1>(),
                javaClass<YHierarchy.B>(),
                javaClass<YHierarchy.C>()
        ))
    }

    fun testDiamondHierarchy() {
        doTest(Classes(
                javaClass<DiamondHierarchy.Top>(),
                javaClass<DiamondHierarchy.A>(),
                javaClass<DiamondHierarchy.A1>(),
                javaClass<DiamondHierarchy.B>(),
                javaClass<DiamondHierarchy.C>()
        ))
    }

    fun testXHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<XHierarchyHollowMiddle.Top1>(),
                javaClass<XHierarchyHollowMiddle.Top2>(),
                javaClass<XHierarchyHollowMiddle.Middle>(),
                javaClass<XHierarchyHollowMiddle.Leaf1>(),
                javaClass<XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    fun testXHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    fun testXHierarchyConflictMiddle() {
        doTest(Classes(
                javaClass<XHierarchyConflictMiddle.Top1>(),
                javaClass<XHierarchyConflictMiddle.Top2>(),
                javaClass<XHierarchyConflictMiddle.Middle>(),
                javaClass<XHierarchyConflictMiddle.Leaf1>(),
                javaClass<XHierarchyConflictMiddle.Leaf2>()
        ))
    }

    fun testAHierarchy() {
        doTest(Classes(
                javaClass<AHierarchy.A>(),
                javaClass<AHierarchy.B>(),
                javaClass<AHierarchy.B1>(),
                javaClass<AHierarchy.C>(),
                javaClass<AHierarchy.C1>()
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

