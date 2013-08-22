package org.jetbrains.kannotator.client

import plume.Options
import java.io.File
import org.jetbrains.kannotator.annotations.io.InferenceParams
import org.jetbrains.kannotator.annotations.io.executeAnnotationTask
import org.jetbrains.kannotator.annotations.io.AnnotatedLibrary

fun argumentsAreInvalid(optionsValues: OptionsHelper, jars: Array<String>?): Boolean =
        jars == null || jars.isEmpty() ||
        optionsValues.output_path == null ||
        !(optionsValues.mutability || optionsValues.nullability)

fun main(args: Array<String>) {
    val optionsValues = OptionsHelper()
    val options = Options("kannotator -o <output path> <jar files...>", optionsValues)
    val jars = options.parse_or_usage(args);

    if (argumentsAreInvalid(optionsValues, jars)){
        System.err.println(options.usage())
    } else {
        val params = InferenceParams(
                optionsValues.nullability,
                optionsValues.mutability,
                optionsValues.output_path!!,
                optionsValues.one_directory_tree,
                jars!!.map { AnnotatedLibrary(it, hashSetOf(File(it))) },
                optionsValues.format!!,
                optionsValues.verbose
        )
        executeAnnotationTask(params, PrintStreamProgressIndicator(params, System.err))
    }
}