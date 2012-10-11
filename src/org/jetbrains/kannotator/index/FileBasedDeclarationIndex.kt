package org.jetbrains.kannotator.index

import java.io.File
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.isStatic
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import util.recurseIntoJars
import java.io.InputStream
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.util.processJar
import java.io.FileInputStream
import java.util.ArrayList
import kotlinlib.recurseFiltered
import java.util.jar.JarFile
import java.util.jar.JarEntry
import kotlinlib.removeSuffix
import org.jetbrains.kannotator.declarations.toFullString
import org.jetbrains.kannotator.asm.util.forEachMethod
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.asm.util.forEachMethodWithMethodVisitor

trait ClassSource {
    fun forEach(body: (ClassReader) -> Unit)
}

class DeclarationIndexImpl(classSource: ClassSource, processMethodBody: (Method) -> MethodVisitor? = {null}): DeclarationIndex, AnnotationKeyIndex {
    private data class ClassData(
            val className: ClassName,
            val methodsById: Map<MethodId, Method>,
            val methodsByName: Map<String, Collection<Method>>
    )

    private val classes = HashMap<ClassName, ClassData>()
    private val classesByCanonicalName = HashMap<String, MutableCollection<ClassData>>();

    { init(classSource, processMethodBody) }

    private fun init(classSource: ClassSource, processMethodBody: (Method) -> MethodVisitor?) {
        classSource forEach {
            reader ->
            val className = ClassName.fromInternalName(reader.getClassName())

            val methodsById = HashMap<MethodId, Method>()
            val methodsByNameForAnnotationKey = HashMap<String, MutableList<Method>>()
            assert (classes[className] == null) { "Class already visited: $className" }
            reader.forEachMethodWithMethodVisitor {
                owner, access, name, desc, signature ->
                val method = Method(className, access, name, desc, signature)
                methodsById[method.id] = method
                methodsByNameForAnnotationKey.getOrPut(method.getMethodNameAccountingForConstructor(), {ArrayList()}).add(method)
                processMethodBody(method)
            }

            val classData = ClassData(className, methodsById, methodsByNameForAnnotationKey)
            classes[className] = classData
            classesByCanonicalName.getOrPut(className.canonicalName, {HashSet()}).add(classData)
        }
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return classes[owner]?.methodsById?.get(MethodId(name, desc))
    }

    override fun findPositionByAnnotationKeyString(annotationKey: String): AnnotationPosition? {
        val (canonicalClassName, _, methodName) = parseMethodAnnotationKey(annotationKey)
        val classes = classesByCanonicalName[canonicalClassName]
        if (classes == null) return null

        for (classData in classes) {
            val methods = classData.methodsByName[methodName]
            if (methods == null) continue
            for (method in methods) {
                for (pos in PositionsWithinMember(method).getValidPositions()) {
                    if (annotationKey == pos.toAnnotationKey()) {
                        return pos
                    }
                }
            }
        }
        return null
    }
}

class FileBasedClassSource(val jarOrClassFiles: Collection<File>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (file in jarOrClassFiles) {
            if (file.isFile()) {
                if (file.getName().endsWith(".jar")) {
                    processJar(file, {f, o, reader -> body(reader)})
                }
                else if (file.getName().endsWith(".class")) {
                    FileInputStream(file) use {body(ClassReader(it))}
                }
            }
        }
    }

}