package inference.propagation

import inferenceData.propagation.*
import junit.framework.TestCase
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.index.ClassSource
import util.Classes

class AnnotationPropagationTest : TestCase() {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            propagateMetadata(methodHierarchy, NullabiltyLattice, initialAnnotations)
        }
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
