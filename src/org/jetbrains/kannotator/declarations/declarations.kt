package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlinlib.suffixAfter
import kotlinlib.suffixAfterLast
import kotlinlib.buildString

data class MethodId(
        val methodName: String,
        val methodDesc: String
) {
    public fun toString(): String = methodName + methodDesc
}

fun MethodId.getReturnType(): Type = Type.getReturnType(methodDesc)
fun MethodId.getArgumentTypes(): Array<Type> = Type.getArgumentTypes(methodDesc) //after KT-2872 should return Array<out Type>
fun MethodId.getArgumentCount(): Int = this.getArgumentTypes().size


enum class Visibility {
    PUBLIC
    PROTECTED
    PACKAGE
    PRIVATE
}

data class Access(private val access: Int) {
    fun has(val flag: Int) = access and flag != 0
    fun toString(): String = "" + Integer.toHexString(access)
}

fun Method(
        declaringClass: ClassName,
        access: Int,
        name: String,
        desc: String,
        signature: String? = null
): Method = Method(declaringClass, Access(access), MethodId(name, desc), signature)

data class Method(
        val declaringClass: ClassName,
        val access: Access,
        val id: MethodId,
        val genericSignature: String? = null
) {
    public fun toString(): String {
        return declaringClass.toType().getClassName() + ":" + id.methodName + id.methodDesc;
    }
}

fun Method.getReturnType(): Type = id.getReturnType()
fun Method.getArgumentTypes(): Array<out Type> = id.getArgumentTypes()

fun Method.isStatic(): Boolean = access.has(Opcodes.ACC_STATIC)

fun Method.isFinal(): Boolean = access.has(Opcodes.ACC_FINAL)

fun Method.isVarargs(): Boolean = access.has(Opcodes.ACC_VARARGS)

val Method.visibility: Visibility get() = when {
    access.has(Opcodes.ACC_PUBLIC) -> Visibility.PUBLIC
    access.has(Opcodes.ACC_PROTECTED) -> Visibility.PROTECTED
    access.has(Opcodes.ACC_PRIVATE) -> Visibility.PRIVATE
    else -> Visibility.PACKAGE
}

fun Method.toFullString(): String {
    return buildString {
        it.append(visibility.toString().toLowerCase() + " ")
        it.append(if (isStatic()) "static " else "")
        it.append(if (isFinal()) "final " else "")
        it.append("flags[$access] ")
        it.append(declaringClass.internal)
        it.append("::")
        it.append(id.methodName)
        it.append(id.methodDesc)
        if (genericSignature != null) {
            it.append(" :: ")
            it.append(genericSignature)
        }
    }
}

data class ClassName private (val internal: String) {
    val typeDescriptor: String
        get() = "L$internal;"

    public fun toString(): String = internal

    val simple: String
        get() = canonicalName.suffixAfterLast(".")

    class object {
        fun fromInternalName(name: String): ClassName {
            return ClassName(name)
        }

        fun fromType(_type: Type): ClassName {
            return ClassName.fromInternalName(_type.getInternalName())
        }
    }
}

val ClassName.canonicalName: String
    get() = internal.internalNameToCanonical()

fun String.internalNameToCanonical(): String = replace('/', '.').toCanonical()

fun String.toCanonical(): String = replace('$', '.')

fun ClassName.toType(): Type {
    return Type.getType(typeDescriptor)
}
