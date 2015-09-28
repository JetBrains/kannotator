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

    @Test fun linearHierarchy() {
        doTest(Classes(
                LinearHierarchy.A::class.java,
                LinearHierarchy.B::class.java,
                LinearHierarchy.C::class.java
        ))
    }

    @Test fun testLinearHierarchyMultipleAnnotations() {
        doTest(Classes(
                LinearHierarchyMultipleAnnotations.A::class.java,
                LinearHierarchyMultipleAnnotations.B::class.java,
                LinearHierarchyMultipleAnnotations.C::class.java
        ))
    }

    @Test fun testLinearHierarchyAlterChildren() {
        doTest(Classes(
                LinearHierarchyAlterChildren.A::class.java,
                LinearHierarchyAlterChildren.B::class.java,
                LinearHierarchyAlterChildren.C::class.java,
                LinearHierarchyAlterChildren.D::class.java
        ))
    }

    @Test fun testLinearHierarchyCovariantReturn() {
        doTest(Classes(
                LinearHierarchyCovariantReturn.A::class.java,
                LinearHierarchyCovariantReturn.B::class.java,
                LinearHierarchyCovariantReturn.C::class.java
        ))
    }

    @Test fun testConflictsAndPropagation() {
        doTest(Classes(
                ConflictsAndPropagation.A::class.java,
                ConflictsAndPropagation.ConflictSource::class.java,
                ConflictsAndPropagation.B::class.java,
                ConflictsAndPropagation.C::class.java
        ))
    }

    @Test fun testLinearHierarchyEmpty() {
        doTest(Classes(
                LinearHierarchy2.A::class.java,
                LinearHierarchy2.B::class.java,
                LinearHierarchy2.C::class.java
        ))
    }

    @Test fun testDiamondHierarchy() {
        doTest(Classes(
                DiamondHierarchy.Top::class.java,
                DiamondHierarchy.A::class.java,
                DiamondHierarchy.A1::class.java,
                DiamondHierarchy.B::class.java,
                DiamondHierarchy.C::class.java
        ))
    }

    @Test fun testXHierarchyAnnotatedMiddle() {
        doTest(Classes(
                XHierarchyAnnotatedMiddle.Top1::class.java,
                XHierarchyAnnotatedMiddle.Top2::class.java,
                XHierarchyAnnotatedMiddle.Middle::class.java,
                XHierarchyAnnotatedMiddle.Leaf1::class.java,
                XHierarchyAnnotatedMiddle.Leaf2::class.java
        ))
    }

    @Test fun testXHierarchyHollowMiddle() {
        doTest(Classes(
                XHierarchyHollowMiddle.Top1::class.java,
                XHierarchyHollowMiddle.Top2::class.java,
                XHierarchyHollowMiddle.Middle::class.java,
                XHierarchyHollowMiddle.Leaf1::class.java,
                XHierarchyHollowMiddle.Leaf2::class.java
        ))
    }

    @Test fun testYHierarchy() {
        doTest(Classes(
                YHierarchy.A::class.java,
                YHierarchy.A1::class.java,
                YHierarchy.B::class.java,
                YHierarchy.C::class.java
        ))
    }


    @Ignore
    @Test fun testTwoHierarchies() {
        doTest(Classes(
                TwoHierarchies.A1::class.java,
                TwoHierarchies.A2::class.java,
                TwoHierarchies.B1::class.java,
                TwoHierarchies.B2::class.java
        ))
    }
}
