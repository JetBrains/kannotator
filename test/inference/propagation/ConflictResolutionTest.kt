package inference.propagation

import inferenceData.propagation.*
import inferenceData.propagation.conflicts
import junit.framework.TestCase
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.index.ClassSource
import util.Classes
import org.jetbrains.kannotator.annotationsInference.propagation.resolveAllAnnotationConflicts
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.AnnotationPosition
import java.util.HashSet
import org.junit.Test

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
                javaClass<conflicts.Conflicts.Base>(),
                javaClass<conflicts.Conflicts.Child>()
        ))
    }

    Test fun linearHierarchy() {
        doTest(Classes(
                javaClass<conflicts.LinearHierarchy.A>(),
                javaClass<conflicts.LinearHierarchy.B>(),
                javaClass<conflicts.LinearHierarchy.C>()
        ))
    }

    Test fun yHierarchy() {
        doTest(Classes(
                javaClass<conflicts.YHierarchy.A>(),
                javaClass<conflicts.YHierarchy.A1>(),
                javaClass<conflicts.YHierarchy.B>(),
                javaClass<conflicts.YHierarchy.C>()
        ))
    }

    Test fun diamondHierarchy() {
        doTest(Classes(
                javaClass<conflicts.DiamondHierarchy.Top>(),
                javaClass<conflicts.DiamondHierarchy.A>(),
                javaClass<conflicts.DiamondHierarchy.A1>(),
                javaClass<conflicts.DiamondHierarchy.B>(),
                javaClass<conflicts.DiamondHierarchy.C>()
        ))
    }

    Test fun xHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyHollowMiddle.Top1>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Top2>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Middle>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    Test fun xHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    Test fun xHierarchyConflictMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyConflictMiddle.Top1>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Top2>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Middle>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Leaf2>()
        ))
    }

    Test fun aHierarchy() {
        doTest(Classes(
                javaClass<conflicts.AHierarchy.A>(),
                javaClass<conflicts.AHierarchy.B>(),
                javaClass<conflicts.AHierarchy.B1>(),
                javaClass<conflicts.AHierarchy.C>(),
                javaClass<conflicts.AHierarchy.C1>()
        ))
    }
}
