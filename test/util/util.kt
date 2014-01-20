package util

import java.io.File
import java.util.ArrayList

import junit.framework.Assert

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type

import kotlinlib.recurseFiltered
import kotlinlib.toUnixSeparators

import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.util.processJar

fun recurseIntoJars(libDir: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    libDir.recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            println("Processing: $file")

            processJar(file, block)
        }
    }
}

fun getAllClassesWithPrefix(prefix: String): ClassSource {
    val result = arrayList<ClassName>()

    findJarsInLibFolder().forEach {
        recurseIntoJars(it) {
            f, classType, classReader ->
            val name = ClassName.fromType(classType)
            if (name.internal.startsWith(prefix)) {
                result.add(name)
            }
        }
    }

    return ClassesFromClassPath(result)
}

fun findJarFiles(dirs: Collection<File>): Collection<File> {
    val jars = ArrayList<File>()
    for (dir in dirs) {
        dir.recurseFiltered({it.extension == "jar"}) {
            jars.add(it)
        }
    }
    return jars
}

fun findJarsInLibFolder(): List<File> {
    val jars = ArrayList<File>()
    File("lib").recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            jars.add(file)
        }
    }
    return jars
}


fun assertEqualsOrCreate(expectedFile: File, actual: String, failOnNoData: Boolean = true): Boolean {
    if (!expectedFile.exists()) {
        expectedFile.getParentFile()!!.mkdirs()
        expectedFile.writeText(actual)
        if (failOnNoData) {
            Assert.fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
        }
        return false
    }

    val expected = expectedFile.readText()

    Assert.assertEquals(expected.toUnixSeparators(), actual.toUnixSeparators())
    return true
}

fun loadNullabilityAnnotations(classSource: ClassSource): Annotations<NullabilityAnnotation> {
    val inferenceResult = inferAnnotations(
            classSource = classSource,
            existingAnnotationFiles = listOf(),
            inferrers = INFERRERS,
            errorHandler = NO_ERROR_HANDLING,
            loadOnly = true,
            propagationOverrides = mapOf(),
            existingAnnotations = mapOf(),
            existingPositionsToExclude = mapOf(),
            packageIsInteresting = {true}
    )
    return inferenceResult.groupByKey[NULLABILITY_KEY]!!.existingAnnotations as Annotations<NullabilityAnnotation>
}