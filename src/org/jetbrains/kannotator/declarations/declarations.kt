package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod

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
}
