package inference.propagation

import inferenceData.propagation.*
import junit.framework.TestCase
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.index.ClassSource
import util.Classes
import org.jetbrains.kannotator.annotationsInference.propagation.resolveAllAnnotationConflicts
import org.jetbrains.kannotator.declarations.AnnotationsImpl

class ConflictResolutionTest: TestCase() {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            val leafMethodNodes = methodHierarchy.nodes.filter{ n -> n.children.isEmpty() }

            val result = AnnotationsImpl(initialAnnotations)

            resolveAllAnnotationConflicts(leafMethodNodes, NullabiltyLattice, result)

            result
        }
    }

    fun testConflicts() {
        doTest(Classes(
                javaClass<conflicts.Conflicts.Base>(),
                javaClass<conflicts.Conflicts.Child>()
        ))
    }

    fun testLinearHierarchy() {
        doTest(Classes(
                javaClass<conflicts.LinearHierarchy.A>(),
                javaClass<conflicts.LinearHierarchy.B>(),
                javaClass<conflicts.LinearHierarchy.C>()
        ))
    }

    fun testYHierarchy() {
        doTest(Classes(
                javaClass<conflicts.YHierarchy.A>(),
                javaClass<conflicts.YHierarchy.A1>(),
                javaClass<conflicts.YHierarchy.B>(),
                javaClass<conflicts.YHierarchy.C>()
        ))
    }

    fun testDiamondHierarchy() {
        doTest(Classes(
                javaClass<conflicts.DiamondHierarchy.Top>(),
                javaClass<conflicts.DiamondHierarchy.A>(),
                javaClass<conflicts.DiamondHierarchy.A1>(),
                javaClass<conflicts.DiamondHierarchy.B>(),
                javaClass<conflicts.DiamondHierarchy.C>()
        ))
    }

    fun testXHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyHollowMiddle.Top1>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Top2>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Middle>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    fun testXHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    fun testXHierarchyConflictMiddle() {
        doTest(Classes(
                javaClass<conflicts.XHierarchyConflictMiddle.Top1>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Top2>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Middle>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Leaf1>(),
                javaClass<conflicts.XHierarchyConflictMiddle.Leaf2>()
        ))
    }

    fun testAHierarchy() {
        doTest(Classes(
                javaClass<conflicts.AHierarchy.A>(),
                javaClass<conflicts.AHierarchy.B>(),
                javaClass<conflicts.AHierarchy.B1>(),
                javaClass<conflicts.AHierarchy.C>(),
                javaClass<conflicts.AHierarchy.C1>()
        ))
    }
}
