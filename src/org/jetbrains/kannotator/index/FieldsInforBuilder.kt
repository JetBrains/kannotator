package org.jetbrains.kannotator.index

import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.asm.util.forEachMethodWithMethodVisitor
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

trait FieldDependencyInfo {
    val field: Field

    val writers: Collection<Method>
    val readers: Collection<Method>
}

data class FieldDependencyInfoImpl(override val field: Field): FieldDependencyInfo {
    override val writers: MutableCollection<Method> = HashSet<Method>()
    override val readers: MutableCollection<Method> = HashSet<Method>()

    public fun toString() : String = "FieldDependencyInfoImpl(field: $field, readers: $readers, writers: $writers)"
}

fun buildFieldsDependencyInfos(declarationIndex: DeclarationIndex, classSource: ClassSource): Map<Field, FieldDependencyInfo> {
    val resultMap = HashMap<Field, FieldDependencyInfoImpl>()

    classSource.forEach {
        reader ->
        reader.forEachMethodWithMethodVisitor { className, access, name, desc, signature ->
            val method = declarationIndex.findMethod(ClassName.fromInternalName(className), name, desc)
            if (method != null) FieldUsageMethodVisitor(method, declarationIndex, resultMap) else null
        }
    }

    return resultMap
}

private class FieldUsageMethodVisitor(val method: Method,
                                      val declarationIndex: DeclarationIndex,
                                      val resultMap: MutableMap<Field, FieldDependencyInfoImpl>): MethodVisitor(ASM4) {
    public override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        val field = declarationIndex.findField(ClassName.fromInternalName(owner), name)
        if (field != null) {
            val checkedField: Field = field
            val fieldInfo = resultMap.getOrPut(checkedField /* Bug here */) { FieldDependencyInfoImpl(field) }

            when (opcode) {
                GETSTATIC, GETFIELD -> fieldInfo.readers.add(method)
                PUTSTATIC, PUTFIELD -> fieldInfo.writers.add(method)
                else -> {}
            }
        }
    }
}