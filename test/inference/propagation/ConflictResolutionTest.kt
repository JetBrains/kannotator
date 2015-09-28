package inference.propagation

import inferenceData.propagation.conflicts.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.resolveAllAnnotationConflicts
import org.jetbrains.kannotator.classHierarchy.children
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.index.ClassSource
import org.junit.Test
import util.Classes
import java.util.*

/** Takes existing annotations and runs conflict resolution (no inference, no full propagation here).
 * Checks against expected annotations.
 * */
class ConflictResolutionTest {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            val leafMethodNodes = methodHierarchy.hierarchyNodes.filter{ n -> n.children.isEmpty() }

            val result = AnnotationsImpl(initialAnnotations)

            resolveAllAnnotationConflicts(leafMethodNodes, NullabiltyLattice, result, HashSet<AnnotationPosition>())

            result
        }
    }

    @Test fun conflicts() {
        doTest(Classes(
                Conflicts.Base::class.java,
                Conflicts.Child::class.java
        ))
    }

    @Test fun linearHierarchy() {
        doTest(Classes(
                LinearHierarchy.A::class.java,
                LinearHierarchy.B::class.java,
                LinearHierarchy.C::class.java
        ))
    }

    @Test fun yHierarchy() {
        doTest(Classes(
                YHierarchy.A::class.java,
                YHierarchy.A1::class.java,
                YHierarchy.B::class.java,
                YHierarchy.C::class.java
        ))
    }

    @Test fun diamondHierarchy() {
        doTest(Classes(
                DiamondHierarchy.Top::class.java,
                DiamondHierarchy.A::class.java,
                DiamondHierarchy.A1::class.java,
                DiamondHierarchy.B::class.java,
                DiamondHierarchy.C::class.java
        ))
    }

    @Test fun xHierarchyHollowMiddle() {
        doTest(Classes(
                XHierarchyHollowMiddle.Top1::class.java,
                XHierarchyHollowMiddle.Top2::class.java,
                XHierarchyHollowMiddle.Middle::class.java,
                XHierarchyHollowMiddle.Leaf1::class.java,
                XHierarchyHollowMiddle.Leaf2::class.java
        ))
    }

    @Test fun xHierarchyAnnotatedMiddle() {
        doTest(Classes(
                XHierarchyAnnotatedMiddle.Top1::class.java,
                XHierarchyAnnotatedMiddle.Top2::class.java,
                XHierarchyAnnotatedMiddle.Middle::class.java,
                XHierarchyAnnotatedMiddle.Leaf1::class.java,
                XHierarchyAnnotatedMiddle.Leaf2::class.java
        ))
    }

    @Test fun xHierarchyConflictMiddle() {
        doTest(Classes(
                XHierarchyConflictMiddle.Top1::class.java,
                XHierarchyConflictMiddle.Top2::class.java,
                XHierarchyConflictMiddle.Middle::class.java,
                XHierarchyConflictMiddle.Leaf1::class.java,
                XHierarchyConflictMiddle.Leaf2::class.java
        ))
    }

    @Test fun aHierarchy() {
        doTest(Classes(
                AHierarchy.A::class.java,
                AHierarchy.B::class.java,
                AHierarchy.B1::class.java,
                AHierarchy.C::class.java,
                AHierarchy.C1::class.java
        ))
    }
}
