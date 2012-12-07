package inference

import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import java.io.File
import java.util.ArrayList
import kotlinlib.*
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.loadAnnotationsFromLogs

fun main(args: Array<String>) {
    val jar = File("lib/jdk_1_7_0_09_rt.jar")

    val declarationIndex = DeclarationIndexImpl(FileBasedClassSource(arrayList(jar)))

    val annotationFiles = ArrayList<File>()
    File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

    val annotations = loadAnnotationsFromLogs(
            arrayList(File("testData/inferenceData/integrated/nullability/jdk_1_7_0_09_rt.jar.annotations.txt")),
            declarationIndex
    )

    val targetDir = File("testData/inferenceData/integrated/kotlinSignatures/root")
    if (targetDir.exists()) {
        targetDir.deleteRecursively()
    }

    writeAnnotationsToXMLByPackage(
            declarationIndex,
            declarationIndex,
            File("lib/jdk-annotations"),
            targetDir,
            annotations
    )
}
