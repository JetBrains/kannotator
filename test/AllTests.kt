package kannotator.tests

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import annotations.io.PositionSerializerTest
import annotations.ReadAnnotationsTest
import classHierarchy.ClassHierarchyTest
import classHierarchy.OverriddenMethodTest
import funDependency.BuildGraphForLibrariesTest
import funDependency.FunDependencyGraphTest
import funDependency.SCCFinderTest
import inference.NullabilityInferenceTest
import inference.MutabilityInferenceTest
import interpreter.InterpreterTest
import annotations.io.AnnotationKeyParserTest
import annotations.io.AnnotationKeyStringMatchingTest
import funDependency.TopologicalSortTest
import index.AnnotationIndexTest
import index.MethodIndexTest
import inference.IntegratedInferenceTest

[RunWith(javaClass<Suite>())]
[SuiteClasses(
        javaClass<PositionSerializerTest>(),
        javaClass<ReadAnnotationsTest>(),
        javaClass<ClassHierarchyTest>(),
        javaClass<OverriddenMethodTest>(),
        javaClass<BuildGraphForLibrariesTest>(),
        javaClass<FunDependencyGraphTest>(),
        javaClass<SCCFinderTest>(),
        javaClass<NullabilityInferenceTest>(),
        javaClass<MutabilityInferenceTest>(),
        javaClass<InterpreterTest>(),
        javaClass<TopologicalSortTest>(),
        javaClass<IntegratedInferenceTest>(),
        javaClass<AnnotationIndexTest>(),
        javaClass<MethodIndexTest>(),
        javaClass<AnnotationKeyStringMatchingTest>(),
        javaClass<AnnotationKeyParserTest>()        
        )]
class AllTests {
}