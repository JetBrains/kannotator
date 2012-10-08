package annotations.io

import java.io.File
import java.io.FileWriter
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedList
import junit.framework.Assert
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotations.io.writeAnnotations
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.internalNameToCanonical
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.util.processJar
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class WriteAnnotationTest {

    Test fun testAll() {
        doTest(array(File("lib")),
                array(File("testData/annotations/write")))
    }

    /*Test fun testJdk() {
        doTest(array(File("c:/Program Files/Java/jdk1.6.0_30/jre/lib/rt.jar")),
                array(File("testData/annotations/jdk-annotations")))
    }*/

    fun doTest(jarDirsOrFiles: Array<File>, annotationDirsOrFiles: Array<File>) {
        val classToReaderMap = HashMap<String, ClassReader>()
        for (dirOrFile in jarDirsOrFiles) {
            dirOrFile.recurseFiltered({ it.extension == "jar" }) {
                file ->
                visitAllInJar(file, {
                    className, classReader ->
                    classToReaderMap.put(className, classReader)
                })
            }
        }

        for (dirOrFile in annotationDirsOrFiles) {
            dirOrFile.recurseFiltered({ it.extension == "xml" }) {
                file ->
                println("Processing ${file.getAbsolutePath()}")
                val typePositionAndAnnotationData = LinkedHashSet<Pair<TypePosition, LinkedList<AnnotationData>>>()
                parseAnnotations(file.reader(), { key, annotationData ->
                    val classReader = classToReaderMap.get(key.substring(0, key.indexOf(" ")))
                    if (classReader != null) {
                        getTypePosition(classReader) {
                            typePosition ->
                            if (typePosition.toAnnotationKey() == key) {
                                typePositionAndAnnotationData.add(Pair(typePosition, annotationData.toLinkedList()))
                            }
                        }
                    }
                    else {
                        println("Cannot find class reader for ${key.substring(0, key.indexOf(" "))} class")
                    }
                }, { str -> println(str) })

                val annotationsList = LinkedList<Annotations<AnnotationData>>()
                annotationsList.add(AnnotationsWithAnnotationData(typePositionAndAnnotationData))

                val actualFile = File.createTempFile("writeAnnotations", file.getName())
                actualFile.createNewFile()
                println("Saved file: ${actualFile.getAbsolutePath()}")
                writeAnnotations<AnnotationData>(FileWriter(actualFile), annotationsList, {
                    annotation -> annotation
                })
                Assert.assertEquals(file.readText().trim(), actualFile.readText().trim())
            }
        }
    }

    fun visitAllInJar(file: File, handler: (String, ClassReader) -> Unit) {
        processJar(file) {
            file, owner, reader ->
            handler(reader.getClassName().internalNameToCanonical(), reader)
        }
    }

    fun getTypePosition(reader: ClassReader, handler: (TypePosition) -> Unit) {
        reader.accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                val method = Method(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature)
                val positions = PositionsWithinMember(method)
                val skip = if (method.isStatic()) 0 else 1
                for (i in skip..method.getArgumentTypes().size) {
                    handler(positions.forParameter(i).position)
                }
                handler(positions.forReturnType().position)
                return null
            }
        }, 0)
    }
}

private class AnnotationsWithAnnotationData(val data: Set<Pair<TypePosition, LinkedList<AnnotationData>>>): Annotations<AnnotationData> {
    override fun forEach(body: (TypePosition, AnnotationData) -> Unit) {
        for ((position, annotations) in data) {
            for (annotation in annotations) {
                body(position, annotation)
            }
        }
    }

    override fun get(typePosition: TypePosition): AnnotationData? {
        throw UnsupportedOperationException()
    }
}



