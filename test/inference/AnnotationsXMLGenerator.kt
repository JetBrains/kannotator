package inference

import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import java.io.File
import java.util.ArrayList
import kotlinlib.*
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.loadAnnotationsFromLogs
import java.io.BufferedReader
import java.io.FileReader
import java.util.Collections
import org.jetbrains.kannotator.main.loadPositionsOfConflictExceptions
import org.jetbrains.kannotator.PRINT_TO_CONSOLE

/** deprecated stuff to generate jdk annotations */
fun main(args: Array<String>) {
    val jarName = if (args.size() == 1) args[0] else "lib/jdk_1_7_0_09_rt.jar"

    val jar = File(jarName)

    val declarationIndex = DeclarationIndexImpl(FileBasedClassSource(listOf(jar)))

    val annotationFiles = ArrayList<File>()
    File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

    val annotations = loadAnnotationsFromLogs(
            listOf(File("testData/inferenceData/integrated/nullability/${jar.getName()}.annotations.txt")),
            declarationIndex
    )

    val includedClassNames = BufferedReader(FileReader(File("testData/inferenceData/integrated/nullability/includedClassNames.txt"))) use { p ->
        p.lineSequence().toSet()
    }

    val includedPositions = loadPositionsOfConflictExceptions(declarationIndex, File("testData/inferenceData/integrated/nullability/includedAnnotationKeys.txt"))

    val targetDir = File("testData/inferenceData/integrated/kotlinSignatures/root")
    if (targetDir.exists()) {
        targetDir.deleteRecursively()
    }

    writeAnnotationsToXMLByPackage(
            declarationIndex,
            declarationIndex,
            File("lib/jdk-annotations"),
            targetDir,
            annotations,
            Collections.emptySet(),
            PRINT_TO_CONSOLE,
            hashSetOf(
                    "java/beans/beancontext",
                    "javax/management/openmbean",
                    "javax/management/remote/rmi/_RMIConnection_Stub",
                    "javax/management/remote/rmi/RMIConnectionImpl_Stub",
                    "org/omg/stub/javax/management/remote/rmi/_RMIConnection_Stub"
            ),
            includedClassNames,
            includedPositions
    )
}
