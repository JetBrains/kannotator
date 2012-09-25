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

public class ClassName private (public val canonical: String) {
    public val internal: String
        get() = canonical.replaceAll("\\.", "/")

    public val typeDescriptor: String
        get() = "L$internal;"

    public fun toString(): String = canonical

    public fun equals(other: Any?): Boolean = other is ClassName && canonical == other.canonical

    public fun hashCode(): Int = canonical.hashCode()

    class object {
        public fun fromCanonicalName(name: String): ClassName {
            return ClassName(name)
        }

        public fun fromInternalName(name: String): ClassName {
            return ClassName(name.replaceAll("/", "."))
        }

        public fun fromClass(clazz: Class<*>): ClassName {
            return ClassName(clazz.getCanonicalName()!!)
        }
    }
}
