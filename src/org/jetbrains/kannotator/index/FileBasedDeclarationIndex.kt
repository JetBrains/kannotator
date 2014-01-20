package org.jetbrains.kannotator.index

import java.io.File
import java.io.FileInputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotations.io.tryParseMethodAnnotationKey
import org.jetbrains.kannotator.annotations.io.tryParseFieldAnnotationKey
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
public fun DeclarationIndexImpl(
        classSource: ClassSource,
        processField: (Field) -> FieldVisitor? = {null},
        processMethodBody: (Method) -> MethodVisitor? = {null},
        failOnDuplicates: Boolean = true,
        delegate: DeclarationIndex? = null
): DeclarationIndexImpl {
    val index = DeclarationIndexImpl(delegate)
    index.addClasses(classSource, processField, processMethodBody, failOnDuplicates)
    return index
}

public class DeclarationIndexImpl(val delegate: DeclarationIndex? = null): DeclarationIndex, AnnotationKeyIndex {

    private data class ClassData(
        val classDecl: ClassDeclaration,
        val methodsById: Map<MethodId, Method>,
        val methodsByName: Map<String, Collection<Method>>,
        val fieldsById: Map<FieldId, Field>
    )

    private val classes = HashMap<ClassName, ClassData>()
    private val classesByCanonicalName = HashMap<String, MutableCollection<ClassData>>();

    public fun addClasses(
            classSource: ClassSource,
            processField: (Field) -> FieldVisitor? = {null},
            processMethodBody: (Method) -> MethodVisitor? = {null},
            failOnDuplicates: Boolean = true
    ) {
        classSource forEach {
            reader ->
            addClass(reader, processField, processMethodBody, failOnDuplicates)
        }
    }

    public fun addClass(
            reader: ClassReader,
            processField: (Field) -> FieldVisitor? = {null},
            processMethodBody: (Method) -> MethodVisitor? = {null},
            failOnDuplicates: Boolean = false
    ) {
        val className = ClassName.fromInternalName(reader.getClassName())

        val methodsById = HashMap<MethodId, Method>()
        val fieldsById = HashMap<FieldId, Field>()
        val methodsByNameForAnnotationKey = HashMap<String, MutableList<Method>>()
        assert (!failOnDuplicates || classes[className] == null) { "Class already visited: $className" }

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
                return processField(field)
            }
        }, 0);

        val classData = ClassData(ClassDeclaration(className, Access(reader.getAccess())), methodsById, methodsByNameForAnnotationKey, fieldsById)
        classes[className] = classData
        classesByCanonicalName.getOrPut(className.canonicalName, { HashSet() }).add(classData)
    }

    override fun findClass(className: ClassName): ClassDeclaration? {
        return classes[className]?.classDecl
                ?: delegate?.findClass(className)
    }

    override fun findMethod(owner: ClassName, name: String, desc: String): Method? {
        return classes[owner]?.methodsById?.get(MethodId(name, desc))
                ?: delegate?.findMethod(owner, name, desc)
    }

    override fun findField(owner: ClassName, name: String): Field? {
        return classes[owner]?.fieldsById?.get(FieldId(name))
                ?: delegate?.findField(owner, name)
    }

    private fun findMethodPositionByAnnotationKeyString(
            annotationKey: String, classes: MutableCollection<ClassData>, methodName: String): AnnotationPosition? {
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

    private fun findFieldPositionByAnnotationKeyString(
            classes: MutableCollection<ClassData>, fieldName: String): AnnotationPosition? {
        val fieldId = FieldId(fieldName)
        for (classData in classes) {
            val field = classData.fieldsById[fieldId]
            if (field == null) continue
            return getFieldTypePosition(field)
        }
        return null
    }

    override fun findPositionByAnnotationKeyString(annotationKey: String): AnnotationPosition? {
        val methodAnnotationKey = tryParseMethodAnnotationKey(annotationKey)
        if (methodAnnotationKey != null) {
            val classes = classesByCanonicalName[methodAnnotationKey.canonicalClassName]
            return if (classes != null) findMethodPositionByAnnotationKeyString(annotationKey, classes, methodAnnotationKey.methodName) else null
        }

        val fieldAnnotationKey = tryParseFieldAnnotationKey(annotationKey)
        if (fieldAnnotationKey != null) {
            val classes = classesByCanonicalName[fieldAnnotationKey.canonicalClassName]
            return if (classes != null) findFieldPositionByAnnotationKeyString(classes, fieldAnnotationKey.fieldName) else null
        }

        // It may be a class key of the form "java.awt.Cursor"
        return null
    }
}

public class FileBasedClassSource(val jarOrClassFiles: Collection<File>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (file in jarOrClassFiles) {
            if (!file.exists()) throw IllegalStateException("File does not exist: $file")
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