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

    Test fun conflicts() {
        doTest(Classes(
                javaClass<Conflicts.Base>(),
                javaClass<Conflicts.Child>()
        ))
    }

    Test fun linearHierarchy() {
        doTest(Classes(
                javaClass<LinearHierarchy.A>(),
                javaClass<LinearHierarchy.B>(),
                javaClass<LinearHierarchy.C>()
        ))
    }

    Test fun yHierarchy() {
        doTest(Classes(
                javaClass<YHierarchy.A>(),
                javaClass<YHierarchy.A1>(),
                javaClass<YHierarchy.B>(),
                javaClass<YHierarchy.C>()
        ))
    }

    Test fun diamondHierarchy() {
        doTest(Classes(
                javaClass<DiamondHierarchy.Top>(),
                javaClass<DiamondHierarchy.A>(),
                javaClass<DiamondHierarchy.A1>(),
                javaClass<DiamondHierarchy.B>(),
                javaClass<DiamondHierarchy.C>()
        ))
    }

    Test fun xHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<XHierarchyHollowMiddle.Top1>(),
                javaClass<XHierarchyHollowMiddle.Top2>(),
                javaClass<XHierarchyHollowMiddle.Middle>(),
                javaClass<XHierarchyHollowMiddle.Leaf1>(),
                javaClass<XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    Test fun xHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    Test fun xHierarchyConflictMiddle() {
        doTest(Classes(
                javaClass<XHierarchyConflictMiddle.Top1>(),
                javaClass<XHierarchyConflictMiddle.Top2>(),
                javaClass<XHierarchyConflictMiddle.Middle>(),
                javaClass<XHierarchyConflictMiddle.Leaf1>(),
                javaClass<XHierarchyConflictMiddle.Leaf2>()
        ))
    }

    Test fun aHierarchy() {
        doTest(Classes(
                javaClass<AHierarchy.A>(),
                javaClass<AHierarchy.B>(),
                javaClass<AHierarchy.B1>(),
                javaClass<AHierarchy.C>(),
                javaClass<AHierarchy.C1>()
        ))
    }
}
