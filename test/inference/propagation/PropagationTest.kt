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

class PropagationTest: TestCase() {

    fun doTest(classSource: ClassSource) {
        doPropagationTest(classSource) {
            methodHierarchy, initialAnnotations ->
            propagateMetadata(methodHierarchy, NullabiltyLattice, initialAnnotations)
        }
    }

    fun testLinearHierarchy() {
        doTest(Classes(
                javaClass<down.LinearHierarchy.A>(),
                javaClass<down.LinearHierarchy.B>(),
                javaClass<down.LinearHierarchy.C>()
        ))
    }

    fun testLinearHierarchyMultipleAnnotations() {
        doTest(Classes(
                javaClass<down.LinearHierarchyMultipleAnnotations.A>(),
                javaClass<down.LinearHierarchyMultipleAnnotations.B>(),
                javaClass<down.LinearHierarchyMultipleAnnotations.C>()
        ))
    }

    fun testLinearHierarchyAlterChildren() {
        doTest(Classes(
                javaClass<down.LinearHierarchyAlterChildren.A>(),
                javaClass<down.LinearHierarchyAlterChildren.B>(),
                javaClass<down.LinearHierarchyAlterChildren.C>(),
                javaClass<down.LinearHierarchyAlterChildren.D>()
        ))
    }

    fun testLinearHierarchyCovariantReturn() {
        doTest(Classes(
                javaClass<down.LinearHierarchyCovariantReturn.A>(),
                javaClass<down.LinearHierarchyCovariantReturn.B>(),
                javaClass<down.LinearHierarchyCovariantReturn.C>()
        ))
    }

    fun testConflictsAndPropagation() {
        doTest(Classes(
                javaClass<down.ConflictsAndPropagation.A>(),
                javaClass<down.ConflictsAndPropagation.ConflictSource>(),
                javaClass<down.ConflictsAndPropagation.B>(),
                javaClass<down.ConflictsAndPropagation.C>()
        ))
    }
}
