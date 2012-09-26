package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type

data class MethodId(
        val methodName: String,
        val methodDesc: String
) {
    public fun toString(): String = methodName + methodDesc
}

fun MethodId.getReturnType(): Type = Type.getReturnType(methodDesc)
fun MethodId.getArgumentTypes(): Array<out Type> = Type.getArgumentTypes(methodDesc) as Array<out Type>

class Method(
        val declaringClass: ClassName,
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
