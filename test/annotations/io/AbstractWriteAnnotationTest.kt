package annotations.io

import java.io.File
import java.util.HashMap
import kotlinlib.prefixUpTo
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.forEachValidPosition
import org.jetbrains.kannotator.declarations.getFieldAnnotatedType
import org.jetbrains.kannotator.declarations.internalNameToCanonical
import org.jetbrains.kannotator.util.processJar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.LinkedHashMap

public open class AbstractWriteAnnotationTest {

    protected fun readAnnotationsForAllPositionsInJarFile(jarFile: File, annotationFile: File): Map<AnnotationPosition, List<AnnotationData>> {
        val classToReaderMap = classToReaderMap(jarFile)
        println("Processing ${annotationFile.getAbsolutePath()}")
        val typePositionAndAnnotationData = LinkedHashMap<AnnotationPosition, MutableList<AnnotationData>>()
        parseAnnotations(annotationFile.reader(), { key, annotationData ->
            val classReader = classToReaderMap.get(key.prefixUpTo(' '))
            if (classReader != null) {
                forAllClassAnnotationPositions(classReader) { annotationPosition ->
                    if (annotationPosition.toAnnotationKey() == key) {
                        for (data in annotationData) {
                            typePositionAndAnnotationData.put(annotationPosition, arrayListOf(data))
                        }
                    }
                }
            }
            else {
                println("Cannot find class reader for ${key.prefixUpTo(' ')} class")
            }
        }, { str -> println(str) })
        return typePositionAndAnnotationData
    }

    private fun classToReaderMap(jarFile: File): Map<String, ClassReader> {
        val classToReaderMap = HashMap<String, ClassReader>()
        visitAllInJar(jarFile, {
            className, classReader ->
            classToReaderMap.put(className, classReader)
        })
        return classToReaderMap
    }

    private fun visitAllInJar(file: File, handler: (String, ClassReader) -> Unit) {
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


