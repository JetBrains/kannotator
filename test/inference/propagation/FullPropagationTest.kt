package inference.propagation

import inferenceData.propagation.*
import java.util.LinkedHashSet
import junit.framework.TestCase
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabiltyLattice
import org.jetbrains.kannotator.annotationsInference.propagation.propagateMetadata
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.ClassSource
import util.Classes
import java.util.HashSet
import org.jetbrains.kannotator.declarations.AnnotationsImpl

class FullPropagationTest: TestCase() {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            propagateMetadata(methodHierarchy, NullabiltyLattice, initialAnnotations, HashSet<AnnotationPosition>(), AnnotationsImpl<NullabilityAnnotation>())
        }
    }

    fun testLinearHierarchy() {
        doTest(Classes(
                javaClass<fullPropagation.LinearHierarchy.A>(),
                javaClass<fullPropagation.LinearHierarchy.B>(),
                javaClass<fullPropagation.LinearHierarchy.C>()
        ))
    }

    fun testLinearHierarchyMultipleAnnotations() {
        doTest(Classes(
                javaClass<fullPropagation.LinearHierarchyMultipleAnnotations.A>(),
                javaClass<fullPropagation.LinearHierarchyMultipleAnnotations.B>(),
                javaClass<fullPropagation.LinearHierarchyMultipleAnnotations.C>()
        ))
    }

    fun testLinearHierarchyAlterChildren() {
        doTest(Classes(
                javaClass<fullPropagation.LinearHierarchyAlterChildren.A>(),
                javaClass<fullPropagation.LinearHierarchyAlterChildren.B>(),
                javaClass<fullPropagation.LinearHierarchyAlterChildren.C>(),
                javaClass<fullPropagation.LinearHierarchyAlterChildren.D>()
        ))
    }

    fun testLinearHierarchyCovariantReturn() {
        doTest(Classes(
                    javaClass<fullPropagation.LinearHierarchyCovariantReturn.A>(),
                javaClass<fullPropagation.LinearHierarchyCovariantReturn.B>(),
                javaClass<fullPropagation.LinearHierarchyCovariantReturn.C>()
        ))
    }

    fun testConflictsAndPropagation() {
        doTest(Classes(
                javaClass<fullPropagation.ConflictsAndPropagation.A>(),
                javaClass<fullPropagation.ConflictsAndPropagation.ConflictSource>(),
                javaClass<fullPropagation.ConflictsAndPropagation.B>(),
                javaClass<fullPropagation.ConflictsAndPropagation.C>()
        ))
    }

    fun testLinearHierarchyEmpty() {
        doTest(Classes(
                javaClass<fullPropagation.LinearHierarchy2.A>(),
                javaClass<fullPropagation.LinearHierarchy2.B>(),
                javaClass<fullPropagation.LinearHierarchy2.C>()
        ))
    }

    fun testDiamondHierarchy() {
        doTest(Classes(
                javaClass<fullPropagation.DiamondHierarchy.Top>(),
                javaClass<fullPropagation.DiamondHierarchy.A>(),
                javaClass<fullPropagation.DiamondHierarchy.A1>(),
                javaClass<fullPropagation.DiamondHierarchy.B>(),
                javaClass<fullPropagation.DiamondHierarchy.C>()
        ))
    }

    fun testXHierarchyAnnotatedMiddle() {
        doTest(Classes(
                javaClass<fullPropagation.XHierarchyAnnotatedMiddle.Top1>(),
                javaClass<fullPropagation.XHierarchyAnnotatedMiddle.Top2>(),
                javaClass<fullPropagation.XHierarchyAnnotatedMiddle.Middle>(),
                javaClass<fullPropagation.XHierarchyAnnotatedMiddle.Leaf1>(),
                javaClass<fullPropagation.XHierarchyAnnotatedMiddle.Leaf2>()
        ))
    }

    fun testXHierarchyHollowMiddle() {
        doTest(Classes(
                javaClass<fullPropagation.XHierarchyHollowMiddle.Top1>(),
                javaClass<fullPropagation.XHierarchyHollowMiddle.Top2>(),
                javaClass<fullPropagation.XHierarchyHollowMiddle.Middle>(),
                javaClass<fullPropagation.XHierarchyHollowMiddle.Leaf1>(),
                javaClass<fullPropagation.XHierarchyHollowMiddle.Leaf2>()
        ))
    }

    fun testYHierarchy() {
        doTest(Classes(
                javaClass<fullPropagation.YHierarchy.A>(),
                javaClass<fullPropagation.YHierarchy.A1>(),
                javaClass<fullPropagation.YHierarchy.B>(),
                javaClass<fullPropagation.YHierarchy.C>()
        ))
    }


    // TODO
    fun todoTestTwoHierarchies() {
        doTest(Classes(
                javaClass<fullPropagation.TwoHierarchies.A1>(),
                javaClass<fullPropagation.TwoHierarchies.A2>(),
                javaClass<fullPropagation.TwoHierarchies.B1>(),
                javaClass<fullPropagation.TwoHierarchies.B2>()
        ))
    }
}
