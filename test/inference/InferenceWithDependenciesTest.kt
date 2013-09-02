package inference

import junit.framework.TestCase
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.io.File
import java.util.ArrayList
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import java.util.Collections
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.main.loadExternalAnnotations
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.declarations.AnnotationPosition
import java.io.FileReader
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import java.io.StringWriter
import org.objectweb.asm.ClassReader
import java.util.LinkedHashMap
import java.util.HashMap
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.annotations.io.AnnotationData
import kotlinlib.join
import util.assertEqualsOrCreate

class InferenceWithDependenciesTest : TestCase() {

    fun testInferenceWithDependencies() {
        val inferrerMap = mapOf(NULLABILITY_KEY to (NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>))

        val baseDir = File("out/test/kannotator")
        val inferenceResult = inferAnnotations(
                FileBasedClassSource(listOf(File(baseDir, "/dependencies/a/A.class"))),
                listOf<File>(),
                inferrerMap,
                ProgressMonitor(),
                NO_ERROR_HANDLING,
                false,
                mapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>()),
                mapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>()),
                {true},
                Collections.emptyMap()
        ) {
            // Load dependencies
            annotationMap, member, declarationIndex ->

            val bClass = File(baseDir, "dependencies/b/B.class")
            declarationIndex.addClass(ClassReader(bClass.readBytes()))
            val external = loadExternalAnnotations(
                    annotationMap,
                    loadAnnotationDataFromRoot(File("testData/dependencies/annotations")),
                    DeclarationIndexImpl(FileBasedClassSource(listOf(bClass))),
                    inferrerMap,
                    NO_ERROR_HANDLING
            )
            for ((k, annotations) in external) {
                annotations.forEach {
                    pos, ann ->
                    annotationMap[NULLABILITY_KEY]!!.set(pos, ann)
                }
            }
            true
        }

        val strings = ArrayList<String>()
        inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations.forEach {
            pos, annotation ->
            strings.add("$pos -> $annotation")
        }
        val actual = strings.sort().join("\n")
        assertEqualsOrCreate(File("testData/dependencies/expected.txt"), actual)
    }

    fun loadAnnotationDataFromRoot(root: File): Collection<() -> FileReader> {
        val result = ArrayList<() -> FileReader>()
        root.recurse {
            file ->
            if (file.extension == "xml") {
                result.add { FileReader(file) }
            }
        }
        return result
    }

}