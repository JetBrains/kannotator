package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type
import org.objectweb.asm.Opcodes

data class MethodId(
        val methodName: String,
        val methodDesc: String
) {
    public fun toString(): String = methodName + methodDesc
}

fun MethodId.getReturnType(): Type = Type.getReturnType(methodDesc)
fun MethodId.getArgumentTypes(): Array<out Type> = Type.getArgumentTypes(methodDesc) as Array<out Type>

enum class MethodKind {
    INSTANCE
    STATIC
}

fun MethodKind(access: Int): MethodKind {
    return if (Opcodes.ACC_STATIC and access == 0) MethodKind.INSTANCE else MethodKind.STATIC
}

fun MethodKind.isStatic(): Boolean = this != MethodKind.INSTANCE


fun Method(
        declaringClass: ClassName,
        access: Int,
        name: String,
        desc: String,
        signature: String? = null
): Method = Method(declaringClass, MethodKind(access), MethodId(name, desc), signature)

class Method(
        val declaringClass: ClassName,
        val kind: MethodKind,
        val id: MethodId,
        val genericSignature: String? = null
) {
    public fun toString(): String {
        return declaringClass.toType().getClassName() + ":" + id.methodName + id.methodDesc;
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is Method) {
            return declaringClass == obj.declaringClass && id == obj.id
        }
        return false
    }

    public fun hashCode(): Int {
        return declaringClass.hashCode() * 31 + id.hashCode()
    }
}

fun Method.getReturnType(): Type = id.getReturnType()
fun Method.getArgumentTypes(): Array<out Type> = id.getArgumentTypes()
fun Method.isStatic(): Boolean = kind.isStatic()

public class ClassName private (public val internal: String) {
    public val canonical: String
        get() = internal.replaceAll("/", "\\.").replaceAll("\$", "\\.")

    public val typeDescriptor: String
        get() = "L$internal;"

    public fun toString(): String = internal

    public val simple: String
        get() = internal.substring(internal.lastIndexOf("/") + 1)

    public fun equals(other: Any?): Boolean = other is ClassName && internal == other.internal

    public fun hashCode(): Int = internal.hashCode()

    class object {
        public fun fromInternalName(name: String): ClassName {
            return ClassName(name)
        }

        public fun fromType(_type: Type): ClassName {
            return ClassName.fromInternalName(_type.getInternalName())
        }
    }
}

fun ClassName.toType(): Type {
    return Type.getType(typeDescriptor)
}
