package inference.propagation

import inferenceData.propagation.fullPropagation.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.index.ClassSource
import org.junit.Ignore
import org.junit.Test
import util.Classes
import java.util.*

/** Takes existing annotations and runs propagation (no inference in this test)
 * Checks against expected annotations.
 * */
class FullPropagationTest {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            propagateMetadata(methodHierarchy, NullabiltyLattice, initialAnnotations, HashSet<AnnotationPosition>(), AnnotationsImpl<NullabilityAnnotation>())
        }
    }

    Test fun linearHierarchy() {
        doTest(Classes(
                javaClass<LinearHierarchy.A>(),
                javaClass<LinearHierarchy.B>(),
                javaClass<LinearHierarchy.C>()
        ))
    }

    Test fun testLinearHierarchyMultipleAnnotations() {
        doTest(Classes(
                javaClass<LinearHierarchyMultipleAnnotations.A>(),
                javaClass<LinearHierarchyMultipleAnnotations.B>(),
                javaClass<LinearHierarchyMultipleAnnotations.C>()
        ))
    }

    Test fun testLinearHierarchyAlterChildren() {
        doTest(Classes(
                javaClass<LinearHierarchyAlterChildren.A>(),
                javaClass<LinearHierarchyAlterChildren.B>(),
                javaClass<LinearHierarchyAlterChildren.C>(),
                javaClass<LinearHierarchyAlterChildren.D>()
        ))
    }

    Test fun testLinearHierarchyCovariantReturn() {
        doTest(Classes(
                    javaClass<LinearHierarchyCovariantReturn.A>(),
                javaClass<LinearHierarchyCovariantReturn.B>(),
                javaClass<LinearHierarchyCovariantReturn.C>()
        ))
    }

    Test fun testConflictsAndPropagation() {
        doTest(Classes(
                javaClass<ConflictsAndPropagation.A>(),
                javaClass<ConflictsAndPropagation.ConflictSource>(),
                javaClass<ConflictsAndPropagation.B>(),
                javaClass<ConflictsAndPropagation.C>()
        ))
    }

    Test fun testLinearHierarchyEmpty() {
        doTest(Classes(
                javaClass<LinearHierarchy2.A>(),
                javaClass<LinearHierarchy2.B>(),
                javaClass<LinearHierarchy2.C>()
        ))
    }

    Test fun testDiamondHierarchy() {
        doTest(Classes(
                javaClass<DiamondHierarchy.Top>(),
                javaClass<DiamondHierarchy.A>(),
                javaClass<DiamondHierarchy.A1>(),
                javaClass<DiamondHierarchy.B>(),
                javaClass<DiamondHierarchy.C>()
        ))
    }

    Test fun testXHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    Test fun testXHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<XHierarchyHollowMiddle.Top1>(),
                javaClass<XHierarchyHollowMiddle.Top2>(),
                javaClass<XHierarchyHollowMiddle.Middle>(),
                javaClass<XHierarchyHollowMiddle.Leaf1>(),
                javaClass<XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    Test fun testYHierarchy() {
        doTest(Classes(
                javaClass<YHierarchy.A>(),
                javaClass<YHierarchy.A1>(),
                javaClass<YHierarchy.B>(),
                javaClass<YHierarchy.C>()
        ))
    }


    Ignore
    Test fun testTwoHierarchies() {
        doTest(Classes(
                javaClass<TwoHierarchies.A1>(),
                javaClass<TwoHierarchies.A2>(),
                javaClass<TwoHierarchies.B1>(),
                javaClass<TwoHierarchies.B2>()
        ))
    }
}
