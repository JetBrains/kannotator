package annotations.io.runner

import org.jetbrains.kannotator.annotations.io.readAnnotationsForAllPositionsInJarFile
import java.io.File
import java.io.FileWriter
import annotations.io.writeAnnotationsJSRStyleGroupedByPackage
import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.*

fun main(args: Array<String>) {
    if (args.size != 3 || args[0] == "-h" || args[0] == "-help") {
        printUsage()
        return
    }
    val jarPath = args[0]
    val jarFile = File(jarPath)
    val annotationsPath = args[1]
    val annotationsFile = File(annotationsPath)
    val outputPath = args[2]
    val outputFile = File(outputPath)
    if (!jarFile.isFile()) {
        reportError("$jarPath is not a file")
        return
    }
    if (!annotationsFile.exists()) {
        reportError("$annotationsPath does not exist")
        return
    }
    outputFile.getParentFile()?.mkdirs()
    val annotationsForAllPositionsInJarFile = readAnnotationsForAllPositionsInJarFile(jarFile, annotationsFile) {
        annotationFqName ->
        annotationFqName == JB_NOT_NULL || annotationFqName == JB_NULLABLE ||
        annotationFqName == JB_READ_ONLY || annotationFqName == JB_MUTABLE
    }
    writeAnnotationsJSRStyleGroupedByPackage(FileWriter(outputFile), annotationsForAllPositionsInJarFile)
    println("Successfully written ${outputFile.getPath()}")
}

fun printUsage() {
    println("Usage: <path_to_jar> <path_to_annotations_in_xml_format_dir_or_file> <path_to_output_jaif_file>")
}

fun reportError(text: String) {
    println("Error: $text.")
    printUsage()
}