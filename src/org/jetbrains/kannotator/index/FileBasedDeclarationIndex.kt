package org.jetbrains.kannotator.index

import java.io.File
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Positions
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
import org.jetbrains.kannotator.annotations.io.parseAnnotationKey

trait ClassSource {
    fun forEach(body: (ClassReader) -> Unit)
}

class DeclarationIndexImpl(classSource: ClassSource): DeclarationIndex {
    private data class ClassData(
            val className: ClassName,
            val methodsById: Map<MethodId, Method>,
            val methodsByName: Map<String, Collection<Method>>
    )

    private val classes = HashMap<ClassName, ClassData>()
    private val classesByCanonicalName = HashMap<String, MutableCollection<ClassData>>();

    { init(classSource) }

    private fun init(classSource: ClassSource) {
        classSource forEach {
            reader ->
            val className = ClassName.fromInternalName(reader.getClassName())

            val methodsById = HashMap<MethodId, Method>()
            val methodsByName = HashMap<String, MutableList<Method>>()
            assert (classes[className] == null) { "Class already visited: $className" }
            reader.forEachMethod {
                owner, access, name, desc, signature ->
                val method = Method(className, access, name, desc, signature)
                methodsById[method.id] = method
                methodsByName.getOrPut(name, {ArrayList()}).add(method)
            }

            val classData = ClassData(className, methodsById, methodsByName)
            classes[className] = classData
            classesByCanonicalName.getOrPut(className.canonicalName, {HashSet()}).add(classData)
        }
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return classes[owner]?.methodsById?.get(MethodId(name, desc))
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