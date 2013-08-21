package org.jetbrains.kannotator.client

import plume.Option
import plume.Options
import java.io.File
import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
import java.util.HashSet
import java.io.PrintWriter
import kotlinlib.toMap
import org.jetbrains.kannotator.annotations.io.InferringParameters


fun <T> List<T>.toSet(): Set<T>
{
    val result = HashSet<T>()
    forEach { result.add(it) }
    return result
}


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
        try{
            val params = InferringParameters(
                    optionsValues.nullability,
                    optionsValues.mutability,
                    optionsValues.output_path!!,
                    optionsValues.one_directory_tree,
                    jars!!.map{ File(it) }.map { it.name to setOf(it) }.toMap(),
                    optionsValues.format!!,
                    optionsValues.verbose
            )
            ConsoleInferringTask(params, System.err).perform()
        }
        catch(e: Throwable) {
            e.printStackTrace()
        }
    }
}