package org.jetbrains.kannotator.index

import java.io.File
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.ClassName
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

class DeclarationIndexImpl(private val classSource: (ClassName) -> ClassReader?): DeclarationIndex {
    private val methodsByClass = HashMap<ClassName, Map<MethodId, Method>>()

    private fun getIdToMethod(className: ClassName): Map<MethodId, Method>? {
        val cached = methodsByClass[className]
        if (cached != null) return cached

        val reader = classSource(className)
        if (reader == null) return null

        val idToMethod = HashMap<MethodId, Method>()
        assert (methodsByClass[className] == null) { "Class already visited: $className" }
        reader.accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                val method = Method(className, access, name, desc, signature)
                idToMethod[method.id] = method
                return null
            }
        }, 0)
        return idToMethod
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return getIdToMethod(owner)?.get(MethodId(name, desc))
    }
}

class FileBasedClassSource(val files: Collection<File>) {
    private data class EntryInJar(val jar: JarFile, val entry: JarEntry)

    private fun EntryInJar.getClassReader() = ClassReader(jar.getInputStream(entry))

    private val fileIterator = files.iterator()
    private val entryCache = HashMap<ClassName, EntryInJar>()

    private fun index(file: File) {
        if (file.isFile()) {
            if (file.getName().endsWith(".jar")) {
                val jarFile = JarFile(file)
                for (entry in jarFile.entries()) {
                    val className = entry!!.getName().removeSuffix(".class")
                    entryCache[ClassName.fromInternalName(className)] = EntryInJar(jarFile, entry!!)
                }
            }
            else {
                throw IllegalArgumentException("Unsupported file extension: ${file.extension}")
            }
        }
    }

    private fun lookUp(className: ClassName): EntryInJar? {
        for (file in fileIterator) {
            index(file)
            val cached = entryCache[className]
            if (cached != null) {
                return cached
            }
        }
        return null
    }

    public fun get(className: ClassName): ClassReader? {
        val cached = entryCache[className]
        if (cached != null) {
            return cached.getClassReader()
        }

        return lookUp(className)?.getClassReader()
    }

}