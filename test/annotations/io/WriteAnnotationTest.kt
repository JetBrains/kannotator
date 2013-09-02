package annotations.io

import java.io.File
import java.io.FileWriter
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedList
import junit.framework.Assert
import kotlinlib.prefixUpTo
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXML
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.forEachValidPosition
import org.jetbrains.kannotator.declarations.getFieldAnnotatedType
import org.jetbrains.kannotator.declarations.internalNameToCanonical
import org.jetbrains.kannotator.util.processJar
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlinlib.toUnixSeparators
import org.jetbrains.kannotator.declarations.Access
import org.jetbrains.kannotator.PRINT_TO_CONSOLE
import java.util.LinkedHashMap

public class WriteAnnotationTest {

    Test fun testAll() {
        doTest(array(File("lib")), array(File("testData/annotations/write")))
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
                val typePositionAndAnnotationData = LinkedHashMap<AnnotationPosition, MutableList<AnnotationData>>()
                parseAnnotations(file.reader(), { key, annotationData ->
                    val classReader = classToReaderMap.get(key.prefixUpTo(' '))
                    if (classReader != null) {
                        forAllClassAnnotationPositions(classReader) { annotationPosition ->
                            if (annotationPosition.toAnnotationKey() == key) {
                                for (data in annotationData) {
                                    typePositionAndAnnotationData.put(annotationPosition, arrayList(data))
                                }
                            }
                        }
                    }
                    else {
                        println("Cannot find class reader for ${key.prefixUpTo(' ')} class")
                    }
                }, PRINT_TO_CONSOLE)


                val actualFile = File.createTempFile("writeAnnotations", file.getName())
                actualFile.createNewFile()
                println("Saved file: ${actualFile.getAbsolutePath()}")
                writeAnnotationsToXML(FileWriter(actualFile), typePositionAndAnnotationData)
                Assert.assertEquals(file.readText().trim().toUnixSeparators(), actualFile.readText().trim().toUnixSeparators())
            }
        }
    }

    fun visitAllInJar(file: File, handler: (String, ClassReader) -> Unit) {
        processJar(file) {
            file, owner, reader ->
            handler(reader.getClassName().internalNameToCanonical(), reader)
        }
    }

    private fun forAllClassAnnotationPositions(classReader: ClassReader, handler: (AnnotationPosition) -> Unit) {
        classReader.accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                val method = Method(ClassName.fromInternalName(classReader.getClassName()), access, name, desc, signature)
                val positions = PositionsForMethod(method)
                positions.forEachValidPosition(handler)
                return null
            }

            public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                val field = Field(ClassName.fromInternalName(classReader.getClassName()), access, name, desc, signature, value)
                handler(getFieldAnnotatedType(field).position)
                return null
            }
        }, 0)
    }
}


