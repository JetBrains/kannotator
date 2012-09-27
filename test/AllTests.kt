package cards.tests

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
import inference.InferenceTest
import interpreter.InterpreterTest

[RunWith(javaClass<Suite>())]
[SuiteClasses(
        javaClass<PositionSerializerTest>(),
        javaClass<ReadAnnotationsTest>(),
        javaClass<ClassHierarchyTest>(),
        javaClass<OverriddenMethodTest>(),
        javaClass<BuildGraphForLibrariesTest>(),
        javaClass<FunDependencyGraphTest>(),
        javaClass<SCCFinderTest>(),
        javaClass<InferenceTest>(),
        javaClass<InterpreterTest>()
        )]
class AllTests {
}