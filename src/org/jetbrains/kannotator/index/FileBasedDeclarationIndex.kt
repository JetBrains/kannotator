package org.jetbrains.kannotator.index

import java.io.File
import java.io.FileInputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.util.processJar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

trait ClassSource {
    fun forEach(body: (ClassReader) -> Unit)
}

class DeclarationIndexImpl(classSource: ClassSource, processMethodBody: (Method) -> MethodVisitor? = {null}): DeclarationIndex, AnnotationKeyIndex {
    private data class ClassData(
        val className: ClassName,
        val methodsById: Map<MethodId, Method>,
        val methodsByName: Map<String, Collection<Method>>,
        val fieldsById: Map<FieldId, Field>
    )

    private val classes = HashMap<ClassName, ClassData>()
    private val classesByCanonicalName = HashMap<String, MutableCollection<ClassData>>();

    { init(classSource, processMethodBody) }

    private fun init(classSource: ClassSource, processMethodBody: (Method) -> MethodVisitor?) {
        classSource forEach { reader ->
            val className = ClassName.fromInternalName(reader.getClassName())

            val methodsById = HashMap<MethodId, Method>()
            val fieldsById = HashMap<FieldId, Field>()
            val methodsByNameForAnnotationKey = HashMap<String, MutableList<Method>>()
            assert (classes[className] == null) { "Class already visited: $className" }

            reader.accept(object: ClassVisitor(Opcodes.ASM4) {
                public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val method = Method(className, access, name, desc, signature)
                    methodsById[method.id] = method
                    methodsByNameForAnnotationKey.getOrPut(method.getMethodNameAccountingForConstructor(), { ArrayList() }).add(method)
                    return processMethodBody(method)
                }

                public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                    val field = Field(className, access, name, desc, signature, value)
                    fieldsById[field.id] = field
                    return null
                }
            }, 0);

            val classData = ClassData(className, methodsById, methodsByNameForAnnotationKey, fieldsById)
            classes[className] = classData
            classesByCanonicalName.getOrPut(className.canonicalName, { HashSet() }).add(classData)
        }
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return classes[owner]?.methodsById?.get(MethodId(name, desc))
    }

    override fun findField(owner: ClassName, name: String): Field? {
        return classes[owner]?.fieldsById?.get(FieldId(name))
    }

    override fun findPositionByAnnotationKeyString(annotationKey: String): AnnotationPosition? {
        val (canonicalClassName, _, methodName) = parseMethodAnnotationKey(annotationKey)
        val classes = classesByCanonicalName[canonicalClassName]
        if (classes == null) return null

        for (classData in classes) {
            val methods = classData.methodsByName[methodName]
            if (methods == null) continue
            for (method in methods) {
                for (pos in PositionsForMethod(method).getValidPositions()) {
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