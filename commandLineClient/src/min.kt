package min

import org.jetbrains.kannotator.*
import org.jetbrains.kannotator.annotations.io.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.*
import org.jetbrains.kannotator.main.*

import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation

import java.io.File
import java.util.ArrayList
import java.util.Date

/* *
 * Script to infer nullability with minimum options (for testing, for jdk)
 * java -cp kannotator-cli.jar min.MinPackage {lib.jar} {outdir} {outfile}
 */
fun main(args: Array<String>) {
    annotateJDK(File(args[0]), File(args[1]), args[2])
}

fun annotateJDK(jdkJar: File, outDir: File, outFile: String) {
    val jarSource = FileBasedClassSource(listOf(jdkJar))
    val declarationIndex = DeclarationIndexImpl(jarSource)

    val inferrer = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
    val inferrers = mapOf(NULLABILITY_KEY to inferrer)
    val inferenceResult = inferAnnotations(
            classSource = jarSource,
            existingAnnotationFiles = listOf(),
            inferrers = inferrers,
            errorHandler = PRINT_TO_CONSOLE,
            loadOnly = false,
            propagationOverrides = mapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>()),
            existingAnnotations = mapOf(),
            packageIsInteresting = { true },
            existingPositionsToExclude = mapOf(),
            progressMonitor = MinProgressIndicator()
    )

    val nullability: InferenceResultGroup<NullabilityAnnotation>  =
            inferenceResult.groupByKey[NULLABILITY_KEY]!! as InferenceResultGroup<NullabilityAnnotation>

    println("Writing to JAIF")

    writeAnnotationsToJaif(
            declarationIndex,
            destRoot = outDir,
            fileName = outFile,
            nullability = nullability.inferredAnnotations,
            propagatedNullabilityPositions = nullability.propagatedPositions,
            includeNullable = true
    )
}

class MinProgressIndicator() : FileAwareProgressMonitor() {

    var numberOfMethods = 0
    var numberOfProcessedMethods = 0
    var progressPercent = -1
    var currentComponent = 0
    var currentIteration = 0

    fun logVerbose(msg: String) {
        println("${Date()} ${msg}")
    }

    override fun processingStarted() {
        logVerbose("Started...")
        numberOfProcessedMethods = 0
    }

    override fun annotationIndexLoaded(index: AnnotationKeyIndex) {
        logVerbose("Index loaded")
    }

    override fun methodsProcessingStarted(methodCount: Int) {
        numberOfMethods = methodCount
        logVerbose("Found ${methodCount} methods")
    }

    override fun processingComponentStarted(methods: Collection<Method>) {
        currentComponent += 1
        currentIteration = 0
        //println()
        //println("Starting component #$currentComponent of ${methods.size()} methods")
        //methods.forEach {println("    $it")}
    }

    override fun processingStepStarted(method: Method) {
        currentIteration += 1
        //println("iteration #$currentIteration")
    }

    override fun processingComponentFinished(methods: Collection<Method>) {
        numberOfProcessedMethods += methods.size

        if (numberOfMethods != 0) {
            val currentProgressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
            if (currentProgressPercent != progressPercent) {
                progressPercent = currentProgressPercent
                logVerbose("  Inferring: $currentProgressPercent% \r");
            }
        } else {
            logVerbose("  Inferring: 100% \n");
        }
    }
}
