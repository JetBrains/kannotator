package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType

public class Method(
    public val declaringClass: Type,
    public val asmMethod: AsmMethod
) {
    public fun toString(): String {
        return declaringClass.getClassName() + ":" + asmMethod;
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is Method) {
            return declaringClass.equals(obj.declaringClass) && asmMethod.equals(obj.asmMethod)
        }
        return false
    }

    public fun hashCode(): Int {
        return declaringClass.hashCode() * 31 + asmMethod.hashCode()
    }

    fun isNeedAnnotating() : Boolean {
        return !asmMethod.getReturnType().isPrimitiveOrVoidType() ||
                !asmMethod.getArgumentTypes().all { it!!.isPrimitiveOrVoidType() }
    }

    public class object {
        public fun create(owner: ClassName, name: String, desc: String): Method {
            val declaringClass = Type.getType(owner.typeDescriptor)
            val asmMethod = AsmMethod(name, desc)
            return Method(declaringClass, asmMethod)
        }
    }
}

public class ClassName private (public val internal: String) {
    public val canonical: String
        get() = internal.replaceAll("/", "\\.")

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
