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
import org.jetbrains.kannotator.declarations.toFullString

trait ClassSource {
    fun forEach(body: (ClassReader) -> Unit)
}

class DeclarationIndexImpl(classSource: ClassSource): DeclarationIndex {
    private val methodsByClass = HashMap<ClassName, MutableMap<MethodId, Method>>();

    { init(classSource) }

    private fun init(classSource: ClassSource) {
        classSource forEach {
            reader ->
            val className = ClassName.fromInternalName(reader.getClassName())
            val idToMethod = HashMap<MethodId, Method>()
            assert (methodsByClass[className] == null) { "Class already visited: $className" }
            methodsByClass[className] = idToMethod
            reader.accept(object : ClassVisitor(Opcodes.ASM4) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val method = Method(className, access, name, desc, signature)
                    idToMethod[method.id] = method
                    return null
                }
            }, 0)
        }
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return methodsByClass[owner]?.get(MethodId(name, desc))
    }
}

class FileBasedClassSource(val files: Collection<File>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (file in files) {
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